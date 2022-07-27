package com.deng.miaosha.controller;

import com.deng.miaosha.cache.LocalCache;
import com.deng.miaosha.controller.viewobject.ItemVO;
import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.error.EmBusinessError;
import com.deng.miaosha.response.CommonReturnType;
import com.deng.miaosha.service.ItemService;
import com.deng.miaosha.service.PromoService;
import com.deng.miaosha.service.model.ItemModel;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/item")
public class ItemController {
    @Autowired
    private ItemService itemService;


    //创建商品
    @PostMapping(value = "/create")
    public CommonReturnType createItem(@RequestParam(name = "title")String title,
                                       @RequestParam(name = "description")String description,
                                       @RequestParam(name = "price") BigDecimal price,
                                       @RequestParam(name = "stock")Integer stock,
                                       @RequestParam(name = "imgUrl")String imgUrl) throws BusinessException {
        //封装到 ItemModel中
        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setDescription(description);
        itemModel.setPrice(price);
        itemModel.setStock(stock);
        itemModel.setImgUrl(imgUrl);

        ItemModel itemModelForReturn = itemService.createItem(itemModel);

        ItemVO itemVO = convertVOFromModel(itemModelForReturn);
        return CommonReturnType.createSuccessReturn(itemVO);
    }


    //根据商品id获取商品详情
    @GetMapping("/get")
    public CommonReturnType getItemById(@RequestParam(name = "id")Integer id) throws BusinessException {
        ItemModel itemModel = itemService.getItemByIdFromCache(id);

        if(itemModel == null){
            throw new BusinessException(EmBusinessError.ITEM_NOT_EXIST);
        }

        ItemVO itemVO = convertVOFromModel(itemModel);

        return CommonReturnType.createSuccessReturn(itemVO);
    }


    //商品列表浏览（获取所有商品）
    @GetMapping("/list")
    public CommonReturnType listItem(){
        List<ItemModel> itemModelList = itemService.listItem();

        //将 List<ItemModel> -> List<ItemVO>
        List<ItemVO> itemVOList = itemModelList.stream().map(itemModel -> {
            return convertVOFromModel(itemModel);
        }).collect(Collectors.toList());

        return CommonReturnType.createSuccessReturn(itemVOList);
    }




    private ItemVO convertVOFromModel(ItemModel itemModel){
        if(itemModel == null){
            return null;
        }
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemModel,itemVO);
        if(itemModel.getPromoModel() != null){  //若有秒杀活动待开始或在进行中
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
            itemVO.setPromoStock(itemModel.getPromoModel().getPromoItemStock());
            //按指定格式将 DateTime对象转为 String
            itemVO.setStartDate(itemModel.getPromoModel().getStartDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return itemVO;
    }
}
