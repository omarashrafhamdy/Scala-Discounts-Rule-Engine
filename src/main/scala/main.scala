import scala.io.Source
import java.time.{Instant, LocalDate, OffsetDateTime}
import java.time.temporal.ChronoUnit
import java.io.{File, FileOutputStream, PrintWriter}
import scala.math.BigDecimal
import java.sql.{Connection, Date, DriverManager, PreparedStatement}
import scala.annotation.tailrec



object main extends App {
  val startTime = System.nanoTime()
  val logTXT: File = new File("src/main/output/logs.txt")
  val txtWriter = new PrintWriter(new FileOutputStream(logTXT, true))
  val ordersWithDiscountsCSV: File = new File("src/main/output/OrdersWithDiscounts.csv")
  val csvWriter = new PrintWriter(new FileOutputStream(ordersWithDiscountsCSV, true))

  log_event(txtWriter, "Info", "Starting Application")
  log_event(txtWriter, "Event", "Opening TRX1000.csv")

  val orders = Source.fromFile("src/main/resources/TRX1000.csv").getLines().toList.tail

  //class for order
  case class Order(orderDate:String,productName:String,expiryDate:String,quantity:Int,unitPrice:Double,channel:String,paymentMethod:String)
  //method to change order string to obj of order
  def toOrder(order:String) :Order =
    Order(order.split(",")(0),order.split(",")(1),order.split(",")(2),order.split(",")(3).toInt,order.split(",")(4).toDouble,order.split(",")(5),order.split(",")(6))

  val ordersWithDiscounts = orders.map(toOrder).map(x=>GetOrdersWithDiscount(x,discountRules()))

  def discountRules(): List[((Order) => Boolean, (Order) => Double)] ={
    List(
      (isMoreThan5ProductsQualified,MoreThan5Products),
      (isProductSoldOn23MarchQualified,ProductSoldOn23March),
      (isCheeseAndWineQualified,CheeseAndWine),
      (isLessThan30DaysQualified,LessThan30Days),
      (isThroughAppQualified,ThroughApp),
      (isUsingVisaQualified,UsingVisa)
    )
  }

  def GetOrdersWithDiscount(order: Order, rules: List[((Order) => Boolean, (Order) => Double)]): String = {
    val startTime = System.nanoTime()
    log_event(txtWriter,"Debug","Start processing")
    val discounts = rules.filter(_._1(order)).map(_._2(order)).sorted.reverse.take(2)
    val finalDiscount =
      if (discounts.isEmpty) 0.00
      else discounts.sum / discounts.length.toDouble

    val endTime = System.nanoTime() // Record end time
    val duration = (endTime - startTime) / 1e6 // Calculate duration in milliseconds


    if (finalDiscount == 0.00)
      log_event(txtWriter,"Info","Didn't qualify for discount")
    else {
      log_event(txtWriter,"Info",s"Customer qualified for discount and qualified for ${discounts.length} rules")
      log_event(txtWriter,"Info",s"Customer discount: $finalDiscount")
    }

    log_event(txtWriter,"Debug",s"Time taken to calculate discount: $duration milliseconds")

    val quantity = order.quantity
    val unitPrice = order.unitPrice
    val finalPrice = (quantity * unitPrice) - (quantity * unitPrice * finalDiscount)
    order.orderDate + "," + order.productName + "," + order.expiryDate + "," + order.quantity + "," + order.unitPrice + "," + order.channel + "," + order.paymentMethod
      + "," + finalDiscount + "," + finalPrice
  }

  //bought more than 5 of the same product
  def isMoreThan5ProductsQualified (order:Order): Boolean = {
    val quantity = order.quantity
    quantity>5
  }

  def MoreThan5Products (order:Order): Double = {
    val quantity = order.quantity
    if (quantity >= 6 && quantity <=9) 0.05 //6 – 9 units -> 5% discount
    else if (quantity >= 10 && quantity <= 14) 0.07 //10-14 units -> 7% discount
    else 0.1 //More than 15 -> 10% discount
  }

  //Products that are sold on 23rd of March have a special discount! (Celebrating the end of java project?)
  def isProductSoldOn23MarchQualified(order: Order): Boolean = {
    val date = order.orderDate.substring(0,10)
    val month = order.orderDate.substring(5,7)
    val day = order.orderDate.substring(8,10)
    month == "03" && day == "23"
  }

  def ProductSoldOn23March(order: Order): Double = {
    val date = order.orderDate.substring(0, 10)
    val month = order.orderDate.substring(5, 7)
    val day = order.orderDate.substring(8, 10)
    if (month == "03" && day == "23") 0.5 //50% discount
    else 0.0
  }

  //Cheese and wine products are on sale
  def isCheeseAndWineQualified(order: Order): Boolean = {
    val productName = order.productName
    productName.startsWith("Wine") || productName.startsWith("Cheese")
  }

  def CheeseAndWine(order: Order): Double = {
    val productName = order.productName
    if (productName.startsWith("Cheese")) 0.1 //cheese -> 10% discount
    else if(productName.startsWith("Wine")) 0.05 //wine -> 5% discount
    else 0.0
  }

  //less than 30 days remaining for the product to expire
  def isLessThan30DaysQualified(order: Order): Boolean = {
    val purchaseDate = LocalDate.parse(order.orderDate.substring(0, 10))
    val expiryDate = LocalDate.parse(order.expiryDate)
    val differenceInDays = ChronoUnit.DAYS.between(purchaseDate, expiryDate)

    differenceInDays < 30
  }

  def LessThan30Days(order: Order): Double = {
    val purchaseDate = LocalDate.parse(order.orderDate.substring(0, 10))
    val expiryDate = LocalDate.parse(order.expiryDate)
    val differenceInDays = ChronoUnit.DAYS.between(purchaseDate, expiryDate)

    @tailrec
    def loop(days:Long, acc:BigDecimal): BigDecimal = {
      if (days >=30) BigDecimal(0.00)
      else {
        if(days == 29) acc + BigDecimal(0.01)
        else loop(days + 1, acc + (0.01))
      }
    }

    val result = loop(differenceInDays, BigDecimal(0.00))
    result.setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  }

  //using app
  def isThroughAppQualified(order: Order): Boolean = {
    val channel = order.channel
    channel == "App"
  }

  def ThroughApp(order: Order): Double = {
    val quantity = order.quantity
    val reminder = quantity % 5
    if (reminder == 0)
      quantity/100.0
    else
      (quantity + (5-reminder))/100.0
  }

  //using visa
  def isUsingVisaQualified(order: Order): Boolean = {
    val payment_method = order.paymentMethod
    payment_method == "Visa"
  }

  def UsingVisa(order: Order): Double = {
    val payment_method = order.paymentMethod
    if (payment_method == "Visa") 0.05
    else 0.0
  }

  //function to writing in db
  def writeToDB(order: String,connection: Connection): Unit = {
    order.split(",").toList match {
      case orderDateStr :: productName :: expiryDateStr :: quantityStr :: unitPriceStr :: channel :: paymentMethod :: discountStr :: finalPriceStr :: Nil =>
        try {
          val orderDate = OffsetDateTime.parse(orderDateStr).toLocalDateTime
          val expiryDate = LocalDate.parse(expiryDateStr)
          val quantity = quantityStr.toInt
          val unitPrice = unitPriceStr.toDouble
          val discount = discountStr.toDouble
          val finalPrice = finalPriceStr.toDouble
          val insertSql =
            "INSERT INTO Orders (Order_Date, Product_Name, Expiry_Date, Quantity, Unit_Price, Channel, Payment_Method, Discount, Final_Price)" +
              " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"

          val preparedStatement = connection.prepareStatement(insertSql)

          preparedStatement.setTimestamp(1, java.sql.Timestamp.valueOf(orderDate))
          preparedStatement.setString(2, productName)
          preparedStatement.setDate(3, java.sql.Date.valueOf(expiryDate))
          preparedStatement.setInt(4, quantity)
          preparedStatement.setDouble(5, unitPrice)
          preparedStatement.setString(6, channel)
          preparedStatement.setString(7, paymentMethod)
          preparedStatement.setDouble(8, discount)
          preparedStatement.setDouble(9, finalPrice)

          preparedStatement.executeUpdate()

          preparedStatement.close()
        } catch {
          case e: Exception =>
            e.printStackTrace()
        }
      case _ => // Handle invalid order format
    }
  }

  //log event function to call it for each log
  def log_event(writer: PrintWriter, log_level: String , message:String): Unit = {
    writer.write(s"Timestamp: ${Instant.now()}\t LogLevel: ${log_level} \t message: $message\n")
    writer.flush()
  }

  //writing result to csv
  def writeResultsToCSV(ordersWithDiscounts: List[String], writer: PrintWriter): Unit = {
    // Write header
    writer.println("Order Date,Product Name,Expiry Date,Quantity,Unit Price,Channel,Payment Method,Discount,Final Price")
    ordersWithDiscounts.foreach(writer.println)
  }

  //a singleton class that have connection method
  object Singleton {
    private val url = "jdbc:oracle:thin:@//localhost:1521/XE"
    private val username = "scala"
    private val password = "123"

    Class.forName("oracle.jdbc.driver.OracleDriver")

    def getConnection(): Connection = {
      DriverManager.getConnection(url, username, password)
    }
  }

  log_event(txtWriter,"Debug", "Writing data to csv")
  writeResultsToCSV(ordersWithDiscounts, csvWriter)
  log_event(txtWriter, "Debug", "Data wrote to csv")


  val dbStartTime = System.nanoTime()
  lazy val connection = Singleton.getConnection()
  log_event(txtWriter,"Debug", "Inserting Data In DB")
  ordersWithDiscounts.foreach(x=>writeToDB(x,connection))
  log_event(txtWriter,"Debug", "Data Inserted In DB")
  connection.close()
  val dbEndTime = System.nanoTime()
  val dbDuration = (dbEndTime - dbStartTime) / 1e6
  log_event(txtWriter,"Debug", s"Time taken to insert in DB: $dbDuration millisecond")

  val endTime = System.nanoTime()
  val duration = (endTime - startTime) / 1e6 // Calculate duration in milliseconds for the whole app
  log_event(txtWriter,"Debug", s"Time taken for the app: $duration millisecond")
  csvWriter.close()
  txtWriter.close()

}
