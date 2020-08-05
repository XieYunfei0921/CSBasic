package main

import (
	"fmt"
	"runtime"
	"sync"
)

var wg sync.WaitGroup

func printPrime(prefix string)  {
	defer wg.Done()
	next:
		for outer:=2;outer<5000;outer++  {
			for inner:=2;inner*inner<=outer ;inner++  {
				if outer%inner==0 {
					continue next;
				}
			}
			fmt.Printf("%s : %d\n",prefix,outer)
		}
		fmt.Println("Completed ",prefix)
}

func main() {
	runtime.GOMAXPROCS(1)
	wg.Add(2)
	fmt.Println("Create Goroutines...")
	go printPrime("A")
	go printPrime("B")
	fmt.Println("Waiting to terminate...")
	wg.Wait()
	fmt.Println("Programmer is finished...")
}
