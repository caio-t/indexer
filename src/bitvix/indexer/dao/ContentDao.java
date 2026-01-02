package bitvix.indexer.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;


import bitvix.indexer.model.Customer;
import bitvix.indexer.model.Site;
import bitvix.indexer.util.Util;
import bitvix.indexer.util.DatabaseException;

public class ContentDao implements WebCrawlerDao {
	
	private Connection conn;
	private PreparedStatement command;
	private LinkedList<Customer> customers = new LinkedList<Customer>();
	
	
	public LinkedList<Site> getDomain(Customer customer) {
		LinkedList<Site> sites = new LinkedList<Site>();
		String sql = "SELECT * FROM entt_site WHERE sit_chp_site IN (SELECT sit_chp_site FROM tbla_customersite WHERE cus_chp_customer = ?)";
		
		try (Connection conn = ConnFactory.conexao(Util.URL, Util.USERNAME, Util.PASSWORD, ConnFactory.MYSQL);
		     PreparedStatement pstm = conn.prepareStatement(sql)) {
			
			pstm.setInt(1, customer.getId());
			ResultSet rs = pstm.executeQuery();
			
			while (rs.next()) {
				Site site = new Site();
				site.setName(rs.getString("sit_var_name"));
				site.setUrl(rs.getString("sit_var_url"));
				site.setDomain(rs.getString("sit_var_domain"));
				site.setMaxDepthOfCrawling(rs.getInt("sit_int_maxdepthofcrawling"));
				site.setMaxPagesToFetch(rs.getInt("sit_int_maxpagestofetch"));
				site.setPolitenessDelay(rs.getInt("sit_int_politenessdelay"));
				sites.add(site);
			}
		} catch (SQLException e) {
			logError("Failed to retrieve sites for customer ID: " + customer.getId(), e);
		} catch (ClassNotFoundException e) {
			logError("Database driver not found", e);
		}
		
		return sites;
	}
	
	@Override
	public void insert(int customerId, String keyword, String anchor, String text, String url, String domain, String subDomain, String parentUrl) {
		String checkSql = "SELECT con_chp_content, con_var_anchor, con_var_text FROM entt_content WHERE cus_chp_customer = ? AND con_var_keyword = ? AND con_var_url = ?";
		String insertSql = "INSERT INTO entt_content (cus_chp_customer, con_var_keyword, con_var_anchor, con_var_text, con_var_url, con_var_domain, con_var_subdomain, con_var_parenturl, con_var_dateregister) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
		String updateSql = "UPDATE entt_content SET con_var_dateregister = NOW(), con_var_anchor = ?, con_var_text = ? WHERE con_chp_content = ?";
		
		try (Connection conn = ConnFactory.conexao(Util.URL, Util.USERNAME, Util.PASSWORD, ConnFactory.MYSQL);
		     PreparedStatement checkStm = conn.prepareStatement(checkSql)) {
			
			checkStm.setInt(1, customerId);
			checkStm.setString(2, keyword);
			checkStm.setString(3, url);
			ResultSet rs = checkStm.executeQuery();
			
			if (!rs.next()) {
				// Content doesn't exist, insert new record
				try (PreparedStatement insertStm = conn.prepareStatement(insertSql)) {
					insertStm.setInt(1, customerId);
					insertStm.setString(2, keyword);
					insertStm.setString(3, anchor);
					insertStm.setString(4, text);
					insertStm.setString(5, url);
					insertStm.setString(6, domain);
					insertStm.setString(7, subDomain);
					insertStm.setString(8, parentUrl);
					insertStm.setQueryTimeout(4000);
					insertStm.execute();
					System.out.println("✓ Content inserted for URL: " + url);
				}
			} else {
				// Content exists, update it
				int contentId = rs.getInt("con_chp_content");
				try (PreparedStatement updateStm = conn.prepareStatement(updateSql)) {
					updateStm.setString(1, anchor);
					updateStm.setString(2, text);
					updateStm.setInt(3, contentId);
					updateStm.setQueryTimeout(2000);
					updateStm.executeUpdate();
					System.out.println("✓ Content updated for URL: " + url);
				}
			}
		} catch (SQLException e) {
			logError("Failed to insert/update content for URL: " + url, e);
		} catch (ClassNotFoundException e) {
			logError("Database driver not found", e);
		}
	}

	public void delete() {
		// TODO Auto-generated method stub
	}

	/**
	 * Logs database errors with context information.
	 * Unlike the old printError method, this does NOT call System.exit(0).
	 * Errors are logged for monitoring/debugging while allowing the application to continue.
	 */
	private void logError(String context, Exception e) {
		System.err.println("❌ DATABASE ERROR: " + context);
		System.err.println("   Cause: " + e.getMessage());
		e.printStackTrace(System.err);
		// Note: Application continues running instead of exiting
		// Callers can implement retry logic or handle the failure gracefully
	}

	public LinkedList<Site> getSitesCustomer(Customer customer) {
		LinkedList<Site> sites = new LinkedList<Site>();
		String sql = "SELECT * FROM entt_site WHERE sit_chp_site IN (SELECT sit_chp_site FROM tbla_customersite WHERE cus_chp_customer = ?)";
		
		try (Connection conn = ConnFactory.conexao(Util.URL, Util.USERNAME, Util.PASSWORD, ConnFactory.MYSQL);
		     PreparedStatement pstm = conn.prepareStatement(sql)) {
			
			pstm.setInt(1, customer.getId());
			ResultSet rs = pstm.executeQuery();
			
			while (rs.next()) {
				Site site = new Site();
				site.setName(rs.getString("sit_var_name"));
				site.setUrl(rs.getString("sit_var_url"));
				site.setDomain(rs.getString("sit_var_domain"));
				site.setMaxDepthOfCrawling(rs.getInt("sit_int_maxdepthofcrawling"));
				site.setMaxPagesToFetch(rs.getInt("sit_int_maxpagestofetch"));
				site.setPolitenessDelay(rs.getInt("sit_int_politenessdelay"));
				sites.add(site);
			}
		} catch (SQLException e) {
			logError("Failed to retrieve sites for customer ID: " + customer.getId(), e);
		} catch (ClassNotFoundException e) {
			logError("Database driver not found", e);
		}
		
		return sites;
	}
	
	public ArrayList<String> getBlackList(Customer customer) {
		ArrayList<String> sites = new ArrayList<String>();
		String sql = "SELECT * FROM entt_blacklist WHERE cus_chp_customer = ?";
		
		try (Connection conn = ConnFactory.conexao(Util.URL, Util.USERNAME, Util.PASSWORD, ConnFactory.MYSQL);
		     PreparedStatement pstm = conn.prepareStatement(sql)) {
			
			pstm.setInt(1, customer.getId());
			ResultSet rs = pstm.executeQuery();
			
			while (rs.next()) {
				sites.add(rs.getString("bll_var_url"));
			}
		} catch (SQLException e) {
			logError("Failed to retrieve blacklist for customer ID: " + customer.getId(), e);
		} catch (ClassNotFoundException e) {
			logError("Database driver not found", e);
		}
		
		return sites;
	}
	
	public ArrayList<String> getSiteSeed(Customer customer) {
		ArrayList<String> sites = new ArrayList<String>();
		String sql = "SELECT * FROM entt_siteseed";
		
		try (Connection conn = ConnFactory.conexao(Util.URL, Util.USERNAME, Util.PASSWORD, ConnFactory.MYSQL);
		     PreparedStatement pstm = conn.prepareStatement(sql)) {
			
			ResultSet rs = pstm.executeQuery();
			
			while (rs.next()) {
				sites.add(rs.getString("sid_var_url"));
			}
		} catch (SQLException e) {
			logError("Failed to retrieve site seeds", e);
		} catch (ClassNotFoundException e) {
			logError("Database driver not found", e);
		}
		
		return sites;
	}
	
	public ArrayList<String> getContentReaded(Customer customer) {
		ArrayList<String> sites = new ArrayList<String>();
		String sql = "SELECT * FROM entt_contentreaded WHERE cus_chp_customer = ?";
		
		try (Connection conn = ConnFactory.conexao(Util.URL, Util.USERNAME, Util.PASSWORD, ConnFactory.MYSQL);
		     PreparedStatement pstm = conn.prepareStatement(sql)) {
			
			pstm.setInt(1, customer.getId());
			ResultSet rs = pstm.executeQuery();
			
			while (rs.next()) {
				sites.add(rs.getString("cor_var_url"));
			}
		} catch (SQLException e) {
			logError("Failed to retrieve content readed for customer ID: " + customer.getId(), e);
		} catch (ClassNotFoundException e) {
			logError("Database driver not found", e);
		}
		
		return sites;
	}
	
	
	
	@Override
	public void fetchCustomer() {
		String sql = "SELECT * FROM entt_customer WHERE cus_int_situation = ?";
		
		try (Connection conn = ConnFactory.conexao(Util.URL, Util.USERNAME, Util.PASSWORD, ConnFactory.MYSQL);
		     PreparedStatement pstm = conn.prepareStatement(sql)) {
			
			pstm.setInt(1, Customer.CUSTOMER_ACTIVE);
			ResultSet rs = pstm.executeQuery();
			LinkedList<Customer> customerList = new LinkedList<Customer>();
			
			while (rs.next()) {
				Customer customer = new Customer();
				customer.setId(rs.getInt("cus_chp_customer"));
				customer.setName(rs.getString("cus_var_name"));
				customer.setKeyword(rs.getString("cus_var_keyword"));
				System.out.println("✓ Loaded customer: " + customer.getName());
				customerList.add(customer);
			}
			
			// Load related data for each customer
			Iterator<Customer> iterator = customerList.iterator();
			while (iterator.hasNext()) {
				Customer customer = iterator.next();
				customer.addSite(this.getSitesCustomer(customer));
				customer.addBlackList(this.getBlackList(customer));
				customer.addSiteSeed(this.getSiteSeed(customer));
				customer.addContentReaded(this.getContentReaded(customer));
				this.customers.add(customer);
			}
		} catch (SQLException e) {
			logError("Failed to fetch customers from database", e);
		} catch (ClassNotFoundException e) {
			logError("Database driver not found", e);
		}
	}
	
	public LinkedList<Customer> getCustomers()
	{
		return this.customers;
	}
	
	public void setCurrentCustomer(Customer customer) {
		String sql = "UPDATE tbla_config SET cof_int_currentcustomer = ?";
		
		try (Connection conn = ConnFactory.conexao(Util.URL, Util.USERNAME, Util.PASSWORD, ConnFactory.MYSQL);
		     PreparedStatement pstm = conn.prepareStatement(sql)) {
			
			pstm.setInt(1, customer.getId());
			pstm.setQueryTimeout(2000);
			pstm.executeUpdate();
			System.out.println("✓ Current customer set to: " + customer.getName());
		} catch (SQLException e) {
			logError("Failed to update current customer", e);
		} catch (ClassNotFoundException e) {
			logError("Database driver not found", e);
		}
	}

	public void updateCountResults(Customer customer) {
		String sql = "UPDATE entt_customer SET cus_int_numberofresults = (SELECT COUNT(con_chp_content) FROM entt_content WHERE cus_chp_customer = ?) WHERE cus_chp_customer = ?";
		
		try (Connection conn = ConnFactory.conexao(Util.URL, Util.USERNAME, Util.PASSWORD, ConnFactory.MYSQL);
		     PreparedStatement pstm = conn.prepareStatement(sql)) {
			
			pstm.setInt(1, customer.getId());
			pstm.setInt(2, customer.getId());
			pstm.setQueryTimeout(4000);
			pstm.executeUpdate();
			System.out.println("✓ Result count updated for customer: " + customer.getName());
		} catch (SQLException e) {
			logError("Failed to update result count for customer ID: " + customer.getId(), e);
		} catch (ClassNotFoundException e) {
			logError("Database driver not found", e);
		}
	}
	

}
