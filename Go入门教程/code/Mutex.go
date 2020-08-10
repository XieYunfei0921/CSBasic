package main

import (
	"fmt"
	"runtime"
	"sync"
)

var(
	counter int
	wg5 sync.WaitGroup
	// 互斥锁
	mutex sync.Mutex
)

func incCounter(id int) {
	defer wg5.Done()
	for count := 0; count<2;count++{
		mutex.Lock()
		{
			value :=counter
			runtime.Gosched()
			value++
			counter=value
		}
		mutex.Unlock()
	}
}	

func main() {
	wg5.Add(2)
	go incCounter(1)
	go incCounter(2)
	wg5.Wait()
	fmt.Printf("Finale Counter: %d \n",counter)
}
