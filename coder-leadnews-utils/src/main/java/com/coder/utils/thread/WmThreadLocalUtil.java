package com.coder.utils.thread;

import com.coder.model.wemedia.pojos.WmUser;

public class WmThreadLocalUtil {

    private final static ThreadLocal<WmUser> WM_USER_THREAD_LOCAL = new ThreadLocal<>();

    //存入线程中
    public static void setUser(WmUser wmUser){
        //每一个线程都有一个自己的ThreadLocalMap，key是当前ThreadLocal对象，value是set的值
        //所以不会出现线程安全问题
        WM_USER_THREAD_LOCAL.set(wmUser);
    }

    //从线程中获取
    public static WmUser getUser(){
        return WM_USER_THREAD_LOCAL.get();
    }

    //清理
    public static void clear(){
        WM_USER_THREAD_LOCAL.remove();
    }

}
