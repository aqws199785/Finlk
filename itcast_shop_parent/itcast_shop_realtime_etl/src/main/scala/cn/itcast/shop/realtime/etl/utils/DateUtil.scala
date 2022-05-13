package cn.itcast.shop.realtime.etl.utils

import java.text.SimpleDateFormat
import java.util.{Date, Locale}

/*
* 时间处理工具类
*/
object DateUtil {
  def dateLoad(time: String): Date = {
    val dateFormat = new SimpleDateFormat("dd/MMM/yyyy:hh:mm:ss", Locale.ENGLISH)
    val date: Date = dateFormat.parse(time)
    println(date)
    date
  }

  // 自定义时间格式化
  //  注意返回值为string
  def customDateFormat(date: Date, format: String): String = {
    val sdf = new SimpleDateFormat(format)
    sdf.format(date)
  }

  def main(args: Array[String]): Unit = {
    println(customDateFormat(dateLoad("05/Sep/2010:11:27:50 +0200"), "yyyy-MM-dd HH:mm:ss"))
  }

}
