package session3.exercise;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class FrontEndDao {
	
	private static FrontEndDao backendDao = new FrontEndDao();
	
	private FrontEndDao(){ }

	public static FrontEndDao getInstance(){
		return backendDao;
	}
	
	/**
	 * 前臺顧客登入查詢
	 * @param identificationNo
	 * @return Member
	 */
	public Member queryMemberByIdentificationNo(String identificationNo){
		Member member = null;
		
		// Select SQL
		String querySQL = "SELECT * FROM BEVERAGE_MEMBER";

		// Step1:取得Connection
		try (Connection conn = DBConnectionFactory.getLocalDBConnection();
			// Step2:Create Statement
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(querySQL)) {
			
			// Step3:Process Result
			while (rs.next()) {
				String password = rs.getString("password");
				String customerName = rs.getString("customer_name");
				member = new Member();
				member.setIdentificationNo(identificationNo);
				member.setPassword(password);
				member.setCustomerName(customerName);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return member;
	}
	
	/**
	 * 前臺顧客瀏灠商品
	 * @param searchKeyword
	 * @param startRowNo
	 * @param endRowNo
	 * @return Set(Goods)
	 */
	public Set<Goods> searchGoods(String searchKeyword, int startRowNo, int endRowNo) {
		Set<Goods> goods = new LinkedHashSet<>();
		
		// Select SQL	
		String querySQL = "SELECT * FROM BEVERAGE_GOODS WHERE goods_id >= ? AND goods_id <= ?";
		// Step1:取得Connection
		try (Connection conn = DBConnectionFactory.getLocalDBConnection();
			// Step2:Create PreparedStatement For SQL
			PreparedStatement pstmt = conn.prepareStatement(querySQL)){
			int count = 1;
			// 設置查詢的欄位值
			pstmt.setInt(count++, startRowNo);
			pstmt.setInt(count++, endRowNo);
			try (ResultSet rs = pstmt.executeQuery()){
				// Step3:Process Result
				while(rs.next()) {
					String searchName = rs.getString("goods_name");
					if(searchName.toLowerCase().contains(searchKeyword.toLowerCase())){
						BigDecimal goodsID = rs.getBigDecimal("goods_id");
						String goodsName = rs.getString("goods_name");
						int goodsPrice = rs.getInt("price");
						int goodsQuantity = rs.getInt("quantity");
						String goodsImageName = rs.getString("image_name");
						String status = rs.getString("status");
						Goods good = new Goods();
						good.setGoodsID(goodsID);
						good.setGoodsName(goodsName);
						good.setGoodsPrice(goodsPrice);
						good.setGoodsQuantity(goodsQuantity);
						good.setGoodsImageName(goodsImageName);
						good.setStatus(status);
						goods.add(good);
					}
				}	
			} catch (SQLException e) {
				throw e;
			}
		} catch (SQLException e) {
					e.printStackTrace();
		}
				
		return goods;
	}
	
	/**
	 * 查詢顧客所購買商品資料(價格、庫存)
	 * @param goodsIDs
	 * @return Map(BigDecimal, Goods)
	 */
	public Map<BigDecimal, Goods> queryBuyGoods(Set<BigDecimal> goodsIDs){
		// key:商品編號、value:商品
		Map<BigDecimal, Goods> goods = new LinkedHashMap<>();
		
		// Select SQL	
		String querySQL = "SELECT * FROM BEVERAGE_GOODS WHERE goods_id = ? ";
		// Step1:取得Connection
		try (Connection conn = DBConnectionFactory.getLocalDBConnection();
			// Step2:Create PreparedStatement For SQL
			PreparedStatement pstmt = conn.prepareStatement(querySQL)){
			// 設置查詢的欄位值
			for(BigDecimal id : goodsIDs){
				int count = 1;
				pstmt.setBigDecimal(count++, id);
				try (ResultSet rs = pstmt.executeQuery()){
					// Step3:Process Result
					while(rs.next()) {
						String goodsName = rs.getString("goods_name");
						int goodsPrice = rs.getInt("price");
						int goodsQuantity = rs.getInt("quantity");
						String goodsImageName = rs.getString("image_name");
						String status = rs.getString("status");
						Goods good = new Goods();
						good.setGoodsID(id);
						good.setGoodsName(goodsName);
						good.setGoodsPrice(goodsPrice);
						good.setGoodsQuantity(goodsQuantity);
						good.setGoodsImageName(goodsImageName);
						good.setStatus(status);
						goods.put(id, good);
					}	
				} catch (SQLException e) {
					throw e;
				}
			}
		} catch (SQLException e) {
					e.printStackTrace();
		}
		
		return goods;
	}
	
	/**
	 * 交易完成更新扣商品庫存數量
	 * @param goods
	 * @return boolean
	 */
	public boolean batchUpdateGoodsQuantity(Set<Goods> goods){
		boolean updateSuccess = false;
		
		String sql = "UPDATE BEVERAGE_GOODS SET quantity = ? WHERE goods_id = ?";
		try(Connection conn = DBConnectionFactory.getLocalDBConnection();
			PreparedStatement pstmt = conn.prepareStatement(sql);) {
			
			// 設置交易不自動提交
			conn.setAutoCommit(false);	
			
			for(Goods good : goods){
				int count = 1;
				// Set the variables
				pstmt.setInt(count++, good.getGoodsQuantity());
				pstmt.setBigDecimal(count++, good.getGoodsID());
						
				// Add it to the batch
				pstmt.addBatch();

			}
			
			pstmt.executeBatch();
			// PS:Oracle 對於 PreparedStatement 批次更新所回傳的異動筆數沒有支援,但資料仍會被異動!
			//需做判斷
			updateSuccess = true;
			
			// 交易提交
			conn.commit();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return updateSuccess;
	}
	
	/**
	 * 建立訂單資料
	 * @param customerID
	 * @param goodsOrders【訂單資料(key:購買商品、value:購買數量)】
	 * @return boolean
	 */
	public boolean batchCreateGoodsOrder(String customerID, Map<Goods,Integer> goodsOrders){
		boolean insertSuccess = false;
		
		String sql = "INSERT INTO BEVERAGE_ORDER (order_id, order_date, customer_id, goods_id, goods_buy_price, buy_quantity)"
					+"VALUES (beverage_order_seq.NEXTVAL, CURRENT_TIMESTAMP, ?, ?, ?, ?)";
		//String cols[] = {"order_id"}; 
		try (Connection conn = DBConnectionFactory.getLocalDBConnection();
			PreparedStatement pstmt = conn.prepareStatement(sql)){
			
			Set<Goods> goodsKey  = goodsOrders.keySet();
			for(Goods key : goodsKey){
				int count = 1;
				// Set the variables
				pstmt.setString(count++, customerID);
				pstmt.setBigDecimal(count++, key.getGoodsID());
				pstmt.setInt(count++, key.getGoodsPrice());
				pstmt.setInt(count++, goodsOrders.get(key));
				// Add it to the batch
				pstmt.addBatch();
			}
			
			pstmt.executeBatch();
			//需做判斷
			insertSuccess = true;
		
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return insertSuccess;
	}	

}
