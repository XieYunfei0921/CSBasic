package main

import (
	"fmt"
	"sync"
	"sync/atomic"
	"time"
)

/**
循环检测shutdown地址位的值, 如果为1则跳出循环并结束, 否则循环打印
 */

var(
	shutdown int64
	wg4 sync.WaitGroup
)

func doWork(name string) {
	defer wg4.Done()
	for {
		fmt.Printf("Doing %s work \n",name)
		time.Sleep(250*time.Millisecond)
		if atomic.LoadInt64(&shutdown)==1 {
			fmt.Printf("Shutting %s down\n",name)
			break
		}
	}
}

func main() {

	wg4.Add(2)
	go doWork("A")
	go doWork("B")
	time.Sleep(2*time.Second)
	fmt.Printf("shut down now...\n")
	// 存储标记位
	atomic.StoreInt64(&shutdown,1)
	wg4.Wait()

}
