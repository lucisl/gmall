package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.constant.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Reference
    private ManageService manageService;

    //添加购物车
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        //根据用户ID和商品ID查询数据库是否有数据
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setUserId(userId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfo);

        if (cartInfoExist != null) {
            //说明当前购物车有该商品,则将该商品的数量相加
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);
            cartInfoExist.setSkuPrice(cartInfoExist.getSkuPrice());
            //更新数据并放到redis缓存中
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);

        } else {
            //说明当前购物车没有该商品
            CartInfo cartInfo1 = new CartInfo();
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            cartInfo1.setSkuId(skuId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuNum(skuNum);
            cartInfoMapper.insertSelective(cartInfo1);
            cartInfoExist = cartInfo1;
        }

        //构建key user:userId:cart
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        //获取jedis对象
        Jedis jedis = redisUtil.getJedis();
        //放入redis
        jedis.hset(userCartKey, skuId, JSON.toJSONString(cartInfoExist));
        jedis.close();
    }

    //根据用户ID查询购物车中商品信息
    @Override
    public List<CartInfo> getCartList(String userId) {

        //定义一个集合
        List<CartInfo> cartInfoList = new ArrayList<>();

        //先查询缓存，如果缓存中有，则直接返回数据
        Jedis jedis = redisUtil.getJedis();
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        List<String> cartJsons = jedis.hvals(userCartKey);
        if (cartJsons != null && cartJsons.size() > 0) {
            //缓存中有数据
            for (String cartJson : cartJsons) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            //排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            System.out.println("走缓存了");
            return cartInfoList;
            //如果缓存中没有数据，则中数据库查询并放入缓存中
        } else {
            cartInfoList = loadCartCache(userId);
            System.out.println("走DB了");
            return cartInfoList;
        }

    }

    //合并购物车
    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) {
        //查询数据库或redis中的购物车列表
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCurPrice(userId);
        boolean isMatch = false;
        if (cartInfoListDB != null && cartInfoListDB.size() > 0) {
            for (CartInfo cartInfoDB : cartInfoListDB) {
                for (CartInfo cartInfoCK : cartListCK) {
                    if (cartInfoCK.getSkuId().equals(cartInfoDB.getSkuId())) {
                        //数据库和cookie中的商品ID一致，数量相加
                        cartInfoDB.setSkuNum(cartInfoCK.getSkuNum() + cartInfoDB.getSkuNum());
                        //更新数据库
                        cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                    } else {
                        //设置userId 将cookie数据插入数据库
                        cartInfoCK.setUserId(userId);
                        cartInfoMapper.insertSelective(cartInfoCK);
                    }
                }
            }
        }
        // 重新在数据库中查询并返回数据
        List<CartInfo> cartInfoList = loadCartCache(userId);

        //合并勾选状态的购物车
        for (CartInfo cartInfoDB : cartInfoList) {
            for (CartInfo cartInfoCK : cartListCK) {
                if (cartInfoDB.getSkuId().equals(cartInfoCK.getSkuId())) {
                    if ("1".equals(cartInfoCK.getIsChecked())) {
                        //将数据库中的对象IsChecked改为1
                        cartInfoDB.setIsChecked(cartInfoCK.getIsChecked());
                        // 更新redis中的isChecked
                        checkCart(cartInfoCK.getSkuId(), "1", userId);
                    }
                }
            }
        }


        return cartInfoList;
    }

    //勾选状态
    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        Jedis jedis = redisUtil.getJedis();
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        String cartJson = jedis.hget(userCartKey, skuId);
        if (StringUtils.isNotEmpty(cartJson)) {
            CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
            cartInfo.setIsChecked(isChecked);
            //将修改之后的cartInfo写入缓冲
            jedis.hset(userCartKey, skuId, JSON.toJSONString(cartInfo));
            //将被选中的商品放入redis中
            String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
            if (isChecked.equals("1")) {
                jedis.hset(userCheckedKey, skuId, JSON.toJSONString(cartInfo));
            } else {
                jedis.hdel(userCheckedKey, skuId);
            }
            jedis.close();
        }
    }

    //根据用户ID查询到被勾选的商品列表
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        //在redis中
        Jedis jedis = redisUtil.getJedis();
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        List<String> cartCheckedList = jedis.hvals(userCheckedKey);

        ArrayList<CartInfo> cartInfoList = new ArrayList<>();

        if (cartCheckedList != null && cartCheckedList.size() > 0) {
            for (String cartJson : cartCheckedList) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
        }
        jedis.close();
        return cartInfoList;
    }

    /**
     * 根据用户ID查询数据库中的数据
     *
     * @param userId
     * @return
     */
    private List<CartInfo> loadCartCache(String userId) {

        List<CartInfo> cartInfos = cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartInfos == null || cartInfos.size() == 0) {
            return null;
        }
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        Map<String, String> map = new HashMap<>();

        //将数据放到缓存中
        for (CartInfo cartInfo : cartInfos) {
            jedis.hset(userCartKey, cartInfo.getSkuId(), JSON.toJSONString(cartInfo));
        }
        jedis.close();
        return cartInfos;
    }
}
