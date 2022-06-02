package exercise;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.LinkedHashSet;
import java.util.Set;

public class BackEndDao {
	
	private static BackEndDao backendDao = new BackEndDao();
	
	private BackEndDao(){ }

	public static BackEndDao getInstance(){
		return backendDao;
	}
	
	/**
	 * 後臺管理商品列表
	 * @return Set(Goods)
	 */
	public Set<Goods> queryGoods() {
		Set<Goods> goods = new LinkedHashSet<>();
		
		// Select SQL	
		String querySQL = "SELECT * FROM BEVERAGE_GOODS";
		
		// Step1:取得Connection
		try (Connection conn = DBConnectionFactory.getLocalDBConnection();
			// Step2:Create Statement
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(querySQL)) {
			
			// Step3:Process Result
			while (rs.next()) {
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
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return goods;
	}
	
	/**
	 * 後臺管理新增商品
	 * @param goods
	 * @return int(商品編號)
	 */
	public int createGoods(Goods goods){
		int goodsID = 0;
		
		// Insert SQL			
		String insertSQL = "INSERT INTO BEVERAGE_GOODS (goods_id, goods_name, price, quantity, image_name, status)"
						  +"VALUES (beverage_goods_seq.NEXTVAL, ?,?,?,?,?)";
		String cols[] = {"goods_id"}; 
		// Step1:取得Connection
		try (Connection conn = DBConnectionFactory.getLocalDBConnection();
			// Step2:Create prepareStatement For SQL
			PreparedStatement pstmt = conn.prepareStatement(insertSQL, cols)){
			
			int count =1;
			// Step3:將"資料欄位編號"、"資料值"作為引數傳入
			pstmt.setString(count++, goods.getGoodsName());
			pstmt.setInt(count++, goods.getGoodsPrice());
			pstmt.setInt(count++, goods.getGoodsQuantity());
			pstmt.setString(count++, goods.getGoodsImageName());
			pstmt.setString(count++, goods.getStatus());
			
			// Step4:Execute SQL
			pstmt.executeUpdate();
			
			// 取對應的自增主鍵值
			ResultSet rsKeys = pstmt.getGeneratedKeys();
			while(rsKeys.next()) goodsID = Integer.parseInt(rsKeys.getString(1));
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return goodsID;
	}
	
	/**
	 * 後臺管理更新商品
	 * @param goods
	 * @return boolean
	 */
	public boolean updateGoods(Goods goods) {
		boolean updateSuccess = false;
		
		// Step1:取得Connection
		try (Connection conn = DBConnectionFactory.getLocalDBConnection()){
			
			// 設置交易不自動提交
			conn.setAutoCommit(false);		
			
			// Update SQL
			String updateSql = "UPDATE BEVERAGE_GOODS SET price = ? WHERE goods_id = ?";
			
			// Step2:Create prepareStatement For SQL
			try (PreparedStatement pstmt = conn.prepareStatement(updateSql)){
				
				int count = 1;
				// Step3:將"資料欄位編號"、"資料值"作為引數傳入
				pstmt.setInt(count++, goods.getGoodsPrice());
				pstmt.setBigDecimal(count++, goods.getGoodsID());
						
				// Step4:Execute SQL			
				if(pstmt.executeUpdate()>0) updateSuccess = true;
				
				// Step5:交易提交
				conn.commit();
				
			} catch (SQLException e) {
				// 發生 Exception 交易資料 roll back
				conn.rollback();
				throw e;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return updateSuccess;
	}
	
	/**
	 * 後臺管理刪除商品
	 * @param goodsID
	 * @return boolean
	 */
	public boolean deleteGoods(BigDecimal goodsID) {
		boolean deleteSuccess = false;
		
		// Step1:取得Connection
		try (Connection conn = DBConnectionFactory.getLocalDBConnection()){
					
			// 設置交易不自動提交
			conn.setAutoCommit(false);
					
			// DELETE SQL
			String deleteSql = "DELETE FROM BEVERAGE_GOODS WHERE goods_id = ?";
					
			// Step2:Create prepareStatement For SQL			
			try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)){
				
				int count = 1;
				// Step3:將"資料欄位編號"、"資料值"作為引數傳入
				pstmt.setBigDecimal(count++, goodsID);
						
				// Step4:Execute SQL
				if(pstmt.executeUpdate()>0) deleteSuccess = true;
						
				// Step5:交易提交
				conn.commit();
						
			} catch (SQLException e) {
				// 若發生錯誤則資料 rollback(回滾)
				conn.rollback();
				throw e;
			}
		} catch (SQLException e) {
				e.printStackTrace();
		}
		return deleteSuccess;
	}
	
	/**
	 * 後臺管理顧客訂單查詢
	 * @param queryStartDate
	 * @param queryEndDate
	 * @return Set(SalesReport)
	 */
	public Set<SalesReport> queryOrderBetweenDate(String queryStartDate, String queryEndDate) {
		Set<SalesReport> reports = new LinkedHashSet<>();
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
		queryStartDate = queryStartDate.replace('/', '-');
		queryEndDate = queryEndDate.replace('/', '-');
		// 使用trunc! Between
		String querySQL = "SELECT O.*, M.customer_name, G.goods_name FROM BEVERAGE_ORDER O"
						+ " LEFT JOIN BEVERAGE_MEMBER M ON O.customer_id = M.identification_no"
						+ " LEFT JOIN BEVERAGE_GOODS G ON O.goods_id = G.goods_id"
						+ " WHERE trunc(O.order_date, 'dd') BETWEEN to_date('"+queryStartDate+"', 'yyyy-MM-dd') AND to_date('"+queryEndDate+"', 'yyyy-MM-dd')"
						+ " ORDER BY O.order_id ASC";
		
		try (Connection conn = DBConnectionFactory.getLocalDBConnection();
			PreparedStatement pstmt = conn.prepareStatement(querySQL);
			ResultSet rs = pstmt.executeQuery()) {
			
			while (rs.next()) {
				long orderID = rs.getLong("order_id");
				String customerName = rs.getString("customer_name");
					
				Timestamp date = rs.getTimestamp("order_date");
				String orderDate = sdf.format(date);
					
				String goodsName = rs.getString("goods_name");
				int goodsBuyPrice = rs.getInt("goods_buy_price");
				int buyQuantity = rs.getInt("buy_quantity");
				int buyAmount = goodsBuyPrice * buyQuantity;
				SalesReport report = new SalesReport();
				report.setOrderID(orderID);
				report.setCustomerName(customerName);
				report.setOrderDate(orderDate);
				report.setGoodsName(goodsName);
				report.setGoodsBuyPrice(goodsBuyPrice);
				report.setBuyQuantity(buyQuantity);
				report.setBuyAmount(buyAmount);
				// orderDate大於等於queryStartDate && orderDate小於等於queryEndDate
				//if(orderDate.compareTo(queryStartDate)>=0 && orderDate.compareTo(queryEndDate)<=0)
				reports.add(report);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return reports;
	}	
	
}
