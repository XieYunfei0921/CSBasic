#include <stdio.h>
#include <string.h>
#define MAX 20

char* s_gets(char* st,int n);

int main(int argc, char const *argv[]){
    char first[MAX];
    char last[MAX];
    char formal[2*MAX+10];
    float prize;
    puts("please input your first name.");
    s_gets(first,MAX);
    puts("please input your last name.");
    s_gets(last,MAX);
    puts("enter your money.");
    // float 格式输入为%f double类型为%lf
    scanf("%f",&prize);
    sprintf(formal,"%s, %s: %.2f\n",last,first,prize);
    puts(formal);
    return 0;
}

char* s_gets(char* st,int n){
    char* ret_val;
    int i=0;
    ret_val=fgets(st,n,stdin);
    if (ret_val){
        while (st[i]!='\n' && st[i]!='\0'){
            i++;
        }
        if(st[i]=='\n')
            st[i]='\0';
        else{
            while (getchar()!='\n'){
                continue;
            }
        }
    }
    return ret_val;
}
