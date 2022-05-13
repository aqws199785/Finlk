package com.itcast.canal_client;

public class Entrance {
    public static void main(String[] args) {
    //    实例化canal的客户端对象 调用start方法拉取canalServer端binlog日志
        CanalClient canalClient = new CanalClient();
        canalClient.start();
    }
}
