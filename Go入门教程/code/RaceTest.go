package main

import (
	"fmt"
	"runtime"
	"sync"
)

/**
	竞争情况的测试
 */
var (
	wg2 sync.WaitGroup
	cnt int
)

func inc(id int)  {
	defer wg2.Done()

	for count:=0;count<2 ;count++ {

		// 取值
		value:=cnt

		// 让出处理器
		runtime.Gosched()

		// 增加本地变量的值
		value++;

		// 写回counter
		cnt=value
	}
}


func main() {

	wg2.Add(2)
	go inc(1)
	go inc(2)
	wg2.Wait()
	fmt.Println("Final Value = ",cnt)
}
