package com.coder.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


@SpringBootApplication
@EnableDiscoveryClient //开启eureka/nacos客户端
@MapperScan("com.coder.user.mapper")
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class,args);
        System.out.println("       用户服务启动成功   ლ(´ڡ`ლ)ﾞ  \\n\" +\n" +
                "                \" .-------.       ____     __        \\n\" +\n" +
                "                \" |  _ _   \\\\      \\\\   \\\\   /  /    \\n\" +\n" +
                "                \" | ( ' )  |       \\\\  _. /  '       \\n\" +\n" +
                "                \" |(_ o _) /        _( )_ .'         \\n\" +\n" +
                "                \" | (_,_).' __  ___(_ o _)'          \\n\" +\n" +
                "                \" |  |\\\\ \\\\  |  ||   |(_,_)'         \\n\" +\n" +
                "                \" |  | \\\\ `'   /|   `-'  /           \\n\" +\n" +
                "                \" |  |  \\\\    /  \\\\      /           \\n\" +\n" +
                "                \" ''-'   `'-'    `-..-'              \");！");
    }
}
