package main

import (
	"fmt"
	"runtime"
	"sync"
	"sync/atomic"
)

/**
	原子函数的测试
 */
var (
	wg3 sync.WaitGroup
	cnt2 int64
)

func inc64(id int)  {
	defer wg3.Done()
	for count:=0;count<2 ;count++ {
		// 同步操作, 保证同一时刻只有一个goroutine执行加法操作
		atomic.AddInt64(&cnt2,1)
		runtime.Gosched()
	}
}


func main() {

	wg3.Add(2)
	go inc64(1)
	go inc64(2)
	wg3.Wait()
	fmt.Println("Final Value = ",cnt2)
}
