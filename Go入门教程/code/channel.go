package main

import "fmt"

func main() {

	// 通道作成

	/**
	使用无缓冲通道的时候, 需要注意同时设置发送goroutine和接受goroutine,否则会面
	临一方阻塞的情况.这种通道的发送和接受是同步的.
	 */

	// 无缓冲通道
	//unbuffered:=make(chan int)
	// 有缓冲区的字符串通道
	buffered:=make(chan string,10)

	buffered<-"sandee"

	// 从通道接受数据
	value:=<-buffered
	fmt.Println(value)
}