package main

import (
	"fmt"
	"runtime"
	"sync"
)

/**
	goroutine的调度算法:
	一个正在运行的goroutine在工作结束之前,可以被停止,并重新调度.这样是防止某个goroutine长期的占用
	处理器.可以使用调度队列,用于只放调度元素.
 */
func main() {
	// 分配一个逻辑处理器给调度器调用
	runtime.GOMAXPROCS(2)

	// 使用变量wg等待线程执行完毕
	var wg sync.WaitGroup
	// 计数器添加2,表示需要等待两个goroutine,相当于java中的CountDownLatch
	wg.Add(2)

	fmt.Println("Start Goroutine...")

	// 声明一个匿名函数,并创建一个goroutine,打印小写字母
	go func() {
		// 在函数退出的时候通知main函数,表示任务已经执行完毕
		// 在这个函数真正执行完成的时候才会正在的调用后边的表达式
		defer wg.Done()
		for cnt := 0; cnt < 3; cnt++ {
			for ch:='a';ch<'a'+26;ch++ {
				fmt.Printf("%c ",ch)
			}
		}
	}()

	// 声明一个匿名函数,并创建一个goroutine,打印大写字母
	go func() {
		// 在函数退出的时候通知main函数,表示任务已经执行完毕
		defer wg.Done()
		for cnt := 0; cnt < 3; cnt++ {
			for ch:='A';ch<'A'+26;ch++ {
				fmt.Printf("%c ",ch)
			}
		}
	}()

	fmt.Println("Waiting to Finish...")
	// 等待计数器减到0,因为交给goroutine执行之后,main函数继续执行,所以可能main先执行返回
	// 这样就看不到后边的结果了,所以需要等待线程执行完毕
	wg.Wait()
	fmt.Println("Finish Program...")
}
