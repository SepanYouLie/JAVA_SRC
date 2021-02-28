import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.PropertyResourceBundle;
import java.io.FileWriter;
import org.postgresql.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.IOException;
import org.json.*;
 
public class test
{
	public static void main(String[] args) {
        test m = new test();
        m.testDatabase();
	}
 
	private void testDatabase() {	//метод получающий набор данных из БД
        
			Document doc=null;								//Готовим Document для формирования в нём HTML с помощью JSoup
			doc = Jsoup.parse("");							//
			Elements els = doc.getElementsByTag("body");									//Делаем заглавный ul
			for (Element el : els) el.appendElement("ul").attr("id", "root").text("Root");	//
			
		ResultSet rs=null,count_types=null; // Создаём два набора для работы с БД
		try {
            PropertyResourceBundle pr = (PropertyResourceBundle) 
            PropertyResourceBundle.getBundle("db"); //получаем ресурсы из файла db.properties
			
			Class.forName("org.postgresql.Driver"); 	//подключаем JDBC драйвер
            String url = pr.getString("URL");			//получаем URL, логин и пароль к БД из db.properties
			String login = pr.getString("login");		//
            String password = pr.getString("password");	//
            Connection con = DriverManager.getConnection(url, login, password);
            String name; //переменная для хранения значения name из столбца data
			Element el;  //элемент для временного хранения родительского элемента HTML
		try {
                Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
                rs = stmt.executeQuery("SELECT *,data->>'name' AS name FROM objects"); //получаем данные из таблицы objects + значение name из json
				
				int columns = rs.getMetaData().getColumnCount();
								
				//здесь строим html, без учёта кто чей родитель, но добавляем служебный аттрибут pid для дальнейшей расстановки по "родству"
				
				while (rs.next()) {	//начинаем идти по ResultSet
                    String str=""; //переменная для вывода в консоль и работы со строками
					
					name = rs.getString("name");
					try{if(rs.getString("name").contentEquals(""))name=null;}catch(Exception e){}  //если в столбце data нет значения name принудительно делаем null
					el = doc.getElementById("root"); //находим головной элемент, которому будем добавлять child
							
							if(name!=null)
							{						
								el.appendElement("ul").attr("pid",rs.getString("parent_object_id"))  //если значение name не null, создаем ul с текстом значения name 
								.attr("id",rs.getString("id")).text(rs.getString("name"));
							} else
								{
								el.appendElement("ul").attr("pid",rs.getString("parent_object_id"))  //если значение name null, создаем ul с текстом uid 
								.attr("id",rs.getString("id")).text(rs.getString("uid"));
								}
					
						for (int i=1;i<=columns;i++)				//Этот кусок можно удалить, нужен только для отладки
							{										//
							str = str+" "+rs.getString(i);			//
                    		}										//
						System.out.println("String " + str);		//
					}
				
					rs.beforeFirst();	
				
				//здесь расставляем элементы по "родству"
				
				while (rs.next()) 
				{
				String cnt=rs.getString("id");
				Element parent;
				if(rs.getString("parent_object_id").equals("0"))
				{
					parent = doc.getElementById("root");
					parent.appendChild(doc.getElementById(cnt));
				}else 
				{	
					parent = doc.getElementById(rs.getString("parent_object_id"));
					parent.appendChild(doc.getElementById(cnt));
				}
				}
				
				//здесь считаем количество элементов с одинаковыми objecn_type и добавляем результат в HTML

				count_types = stmt.executeQuery("SELECT MAX(object_type) AS Type, COUNT(object_type) "
				+"AS Count FROM objects GROUP BY (object_type) ORDER BY Type");
				String str="";
				while (count_types.next()) 
				{
					str = "Type "+count_types.getString("Type")+": "+count_types.getString("Count");
					System.out.println(str);
					doc.getElementById("root").appendElement("h3").attr("id", "count").text(str);
				}
				
				count_types.close();
				rs.close();
				stmt.close();
			} finally {
					con.close();
				}
		} catch (Exception e) {
            e.printStackTrace();
        }
		
		//здесь создаём index.html и записываем в него Document doc, в котором сформирован HTML код
		
		FileWriter fr=null;
		try{fr = new FileWriter("index.html");}catch(IOException e){}
		
		/
		try 
		{
			fr.write(doc.outerHtml().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            try {
        
				fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
		}
	}
}