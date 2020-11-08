#include <stdio.h>

struct info{
    int value;
    int nums;
};

/*每次计算一个数值的1的数量,进行局部排序即可*/
int* sortByBits(int* arr, int arrSize, int* returnSize){
    struct info infos[arrSize];
    int ops=0,point=0,cnt=0;
    printf("tag \n");
    while (point<arrSize){
        cnt++;
        infos[point].value=*(arr+point);
        ops=*(arr+point);
        while (ops>=1){
            if(ops&1==0)
                cnt++;
            ops>>1;
        }
        infos[point].nums=cnt;
        point++;
    }
    // 结构体排序
    
    struct info temp;
    for (size_t i = 0; i < arrSize; i++){
        for (size_t j = i+1; j < arrSize; j++){
            if(infos[j].nums<infos[i].nums){
                temp=infos[j];
                infos[j]=infos[i];
                infos[i]=temp;
            }
        }
    }
    point=0;
    int* res;
    while (point<arrSize){
        *(res+point)=infos[point].value;
        point++;
    }
    return res;
    
}

int main(int argc, char const *argv[]){

    int arr[500]={0,1,2,3,4,5,6,7,8};
    int* res=sortByBits(arr,9,NULL);
    for (size_t i = 0; i < 9; i++){
        printf("%d\t",*(res+i));
    }

    return 0;
}
