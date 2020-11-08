#include <stdio.h>
#include <stdlib.h>

int vlamal();

int main(int argc, char const *argv[]){
    
    vlamal();

    return 0;
}

int vlamal(){
    int n;//变长参数
    int* pi;
    scanf("%d",&n);
    pi=(int *)malloc(n*sizeof(int));
    int arr[n];
    // 程序离开变长数组定义的内存块时候,数组的空间会被释放
    int p=0;
    while (p<n){
        pi[p]=arr[p]=p;
        p++;
    }
    printf("in memory block: %d \n",pi[1]);
    arr[n]=n+100;
}