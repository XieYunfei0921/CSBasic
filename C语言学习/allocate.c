#include <stdio.h>
#include <stdlib.h>
int main(int argc, char const *argv[]){
    float* ptd;
    
    ptd=(double *)malloc(30*sizeof(double)); 
    *ptd=3.57;
    *(ptd+1)=7.28;
    *(ptd+2)=11.53;
    printf("ptd2= %f\n",*ptd);
    free(ptd);
    if(ptd){
        printf("allocated address is %x\n",ptd);
    }else{
        printf("allocate on failure.\n");
    }
    printf("ptd2= %f\n",*ptd);
    return 0;
}
