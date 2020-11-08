#include <stdio.h>

// 外部变量~成员变量
int errupt;// 外部定义变量
double up[100];// 外部定义数组
extern char coal;
int units;

int main(int argc, char const *argv[]){
    // register int quick;
    printf("How many golds do you have?\n");
    scanf("%d",&units);
    while (units!=56){
        critic();
    }
    printf("you have a bonus!");
    return 0;
}

void critic(void){
    printf("unluck,please retry.\n");
    scanf("%d",&units);
}
