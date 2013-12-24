package dao;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

/**
 * <p>The data operate class, utilize hibernate as
 * DAO level.</p>
 * 
 * <p>All methods in this class will be synchronized 
 * by sessiionfactory, because this class may be 
 * instantiated to many objects in multiple thread, 
 * so this class must guarantee the accuracy of 
 * all operations to the database.</p>
 * 
 * <p>author: 	Piasy Xu</p>
 * <p>date:		14:13 2013/12/15</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 */
public class DataOperator 
{
	static SessionFactory sessionFactory;

	static 
	{
		try 
		{
			Configuration config = new Configuration();
			config.configure();	//load hibernate.cfg.xml configuration file
			
			sessionFactory = config.buildSessionFactory();
		} 
		catch (RuntimeException e) 
		{
			e.printStackTrace();
			throw e;
		}

	}
	
	/**
	 * The query method, get all records in the table `files`.
	 * @return The list that contain all records in the table `files`
	 * */
	public List<FileEntry> query()
	{
		synchronized (sessionFactory)
		{
			Session session = sessionFactory.openSession();
			Transaction transection = null;
			List<FileEntry> entryList = new ArrayList<FileEntry>();
			try 
			{
				transection = session.beginTransaction();
				Query query = session.createQuery("from FileEntry");
				
				@SuppressWarnings("rawtypes")
				List list = query.list();

				FileEntry entry;
				for (int i = 0; i < list.size(); i++) 
				{
					entry = (FileEntry) list.get(i);
					entryList.add(entry);
				}
				transection.commit();

			} 
			catch (RuntimeException e) 
			{
				if (transection != null) 
				{
					transection.rollback();
				}
				throw e;
			} 
			finally 
			{
				session.close();
			}	
			return entryList;
		}
	}
	
	/**
	 * The query method, get records in the table `files` where `column` equals `value`.
	 * @return The list that contain records in the table `files` meeting the criteria
	 * */
	public List<FileEntry> queryBy(String column, String value)
	{
		synchronized (sessionFactory)
		{
			Session session = sessionFactory.openSession();
			Transaction transection = null;
			List<FileEntry> entryList = new ArrayList<FileEntry>();
			try 
			{
				transection = session.beginTransaction();
				Query query = session.createQuery("from FileEntry where " + column + " = ?");
				query.setString(0, value);
				
				@SuppressWarnings("rawtypes")
				List list = query.list();

				FileEntry entry;
				for (int i = 0; i < list.size(); i++) 
				{
					entry = (FileEntry) list.get(i);
					entryList.add(entry);
				}
				transection.commit();

			} 
			catch (RuntimeException e) 
			{
				if (transection != null) 
				{
					transection.rollback();
				}
				throw e;
			} 
			finally 
			{
				session.close();
			}	
			return entryList;
		}
	}
	
	/**
	 * The method that will insert an entry to the table
	 * @param The entry to be inserted.
	 * @return The result that whether the insertion is successful.
	 * */
	public boolean insert(FileEntry entry)
	{
		synchronized (sessionFactory)
		{
			boolean result = false;
			Session session = sessionFactory.openSession();
			Transaction transection = null;
			try 
			{
				transection = session.beginTransaction();
				session.save(entry);
				transection.commit();
				result = true;
			} 
			catch (RuntimeException e) 
			{
				if (transection != null) 
				{
					transection.rollback();
				}
				throw e;
			} 
			finally 
			{
				session.close();
			}
			return result;	
		}
	}
	
	/**
	 * The method that will update an entry to the table.
	 * The upper level must prepare the id itself.
	 * @param The entry to be updated.
	 * @return The result that whether the operation is successful.
	 * */
	public boolean update(FileEntry entry)
	{
		synchronized (sessionFactory)
		{
			boolean result = false;
			Session session = sessionFactory.openSession();
			Transaction transection = null;
			try 
			{
				transection = session.beginTransaction();
				session.update(entry);
				transection.commit();
				result = true;
			} 
			catch (RuntimeException e) 
			{
				if (transection != null) 
				{
					transection.rollback();
				}
				throw e;
			} 
			finally 
			{
				session.close();
			}
			return result;	
		}
	}
	
	/**
	 * The method that will delete an entry to the table
	 * The upper level must prepare the id itself.
	 * @param The entry to be deleted.
	 * @return The result that whether the operation is successful.
	 * */
	public boolean delete(FileEntry entry)
	{
		synchronized (sessionFactory)
		{
			boolean result = false;
			Session session = sessionFactory.openSession();
			Transaction transection = null;
			try 
			{
				transection = session.beginTransaction();
				session.delete(entry);
				transection.commit();
				result = true;
			} 
			catch (RuntimeException e) 
			{
				if (transection != null) 
				{
					transection.rollback();
				}
				throw e;
			} 
			finally 
			{
				session.close();
			}
			return result;	
		}
	}
}
