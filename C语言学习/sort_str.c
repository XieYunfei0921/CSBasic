#include <stdio.h>
#include <string.h>
#define SIZE 81
#define LIM 20
#define HALT ""

char* s_gets(char* st,int n);
void ststr(char* string[],int num);

int main(int argc, char const *argv[]){
    
    char input[LIM][SIZE];
    char* prstr[LIM];
    int ct=0;
    int k;
    while (ct<LIM && s_gets(input[ct],SIZE)!=NULL && input[ct][0]!='\0'){
        // 将指针指向输入字符串
        prstr[ct]=input[ct];
        ct++;
    }
    ststr(prstr,ct);
    puts("Sorted List is as following.");
    for (size_t i = 0; i < ct; i++){
        puts(prstr[k]);
    }
    
    return 0;
}

void ststr(char* strings[],int num){
    char* temp;
    int top,seek;
    for (size_t top = 0; top < num-1; top++){
        for (size_t seek = top+1; seek < num; seek++){
            if(strcmp(strings[top],strings[seek])>0){
                temp=strings[top];
                strings[top]=strings[seek];
                strings[seek]=temp;
            }
        }
    }
    printf("sort completely...\n");
    
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