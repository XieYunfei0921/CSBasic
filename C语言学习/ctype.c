#include <stdio.h>
#include <string.h>
#include <ctype.h>

void ToUpper(char* s);

int main(int argc, char const *argv[]){

    char *s="i am sandee.who are you?i am fine,thank you!";
    // ToUpper(s);
    s[2]='B';
    puts(s);
    // printf("%c\n",toupper(*(s+2)));
    return 0;
}

void ToUpper(char* s){
    int p=0;char temp;
    while (*(s+p)!='\0'){
        temp=*(s+p);
        printf("res %c : %c\n",*(s+p),toupper(temp));
        *(s+p)='a';
        p++;
    }
}