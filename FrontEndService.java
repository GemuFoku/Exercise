package session3.exercise;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FrontEndService {

	public static void main(String[] args) {
		// 請先執行 BEVERAGE.sql 至 Local DB
		FrontEndDao frontEndDao = FrontEndDao.getInstance();
		
		// 1.前臺顧客登入查詢
		String customerID = "D201663865";
		Member member = frontEndDao.queryMemberByIdentificationNo(customerID);
		System.out.println(member);
		System.out.println("----------------------------------------");
		
		// 2.前臺顧客瀏灠商品
		Set<Goods> goods = frontEndDao.searchGoods("ca", 6, 8);
		goods.stream().forEach(g -> System.out.println(g));
		System.out.println("----------------------------------------");
		
		// 3.前臺顧客購買建立訂單
		// Step1:查詢顧客所購買商品資料(價格、庫存)
		Set<BigDecimal> goodsIDs = new HashSet<>();
		goodsIDs.add(new BigDecimal("19"));
		goodsIDs.add(new BigDecimal("20"));
		Map<BigDecimal, Goods> buyGoods = frontEndDao.queryBuyGoods(goodsIDs);
		buyGoods.values().stream().forEach(g -> System.out.println(g));		
		
		// Step2:建立訂單資料
		// 訂單資料(key:購買商品、value:購買數量)
		int buyQuantity = 2; // 購買數量皆為2
		Map<Goods,Integer> goodsOrders = new HashMap<>();
		goodsIDs.stream().forEach(goodsID -> {
			Goods g = buyGoods.get(goodsID);	
			goodsOrders.put(g, buyQuantity); 
		});
		// 建立訂單
		boolean insertSuccess = frontEndDao.batchCreateGoodsOrder(customerID, goodsOrders);
		if(insertSuccess){System.out.println("建立訂單成功!");}
		
		// Step3:交易完成更新扣商品庫存數量
		// 將顧客所購買商品扣除更新商品庫存數量
		buyGoods.values().stream().forEach(g -> g.setGoodsQuantity(g.getGoodsQuantity() - buyQuantity));
		boolean updateSuccess = frontEndDao.batchUpdateGoodsQuantity(buyGoods.values().stream().collect(Collectors.toSet()));
		if(updateSuccess){System.out.println("商品庫存更新成功!");}
		
	}

}
