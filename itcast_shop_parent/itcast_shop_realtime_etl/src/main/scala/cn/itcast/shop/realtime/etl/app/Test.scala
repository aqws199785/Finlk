package cn.itcast.shop.realtime.etl.app

object Test {
  def main(args: Array[String]): Unit = {
    val str = "上海市"
    val str1: String = str.replaceAll("省|市|乡|村|区", "-")
    println(str1)
    val str2: Array[String] = str1.split("-")
    println(str2(0),str2(1))
  }
}
