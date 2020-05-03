#include <iostream>
#include <string>
using namespace std;
/*
    KMP算法的实现
*/
const int MAXN=10010;
int next[MAXN];
// 获取next数组
void getNext(string s){
    fill(next,next+MAXN,-2);
    next[0]=-1;
    int i=1,j=-1;
    while (i<s.length()){
        while (j!=-1 && s[i]!=s[j+1]){
            j=next[j];
        }
        if(s[i]==s[j+1]) j++;
        next[i]=j;
        i++;
    }
}
/*
    @s 主串
    @p 模式串 
*/
int KMP(string s,string p){
    int slen=s.length();
    int plen=p.length();
    int j=-1;
    next[0]=-1;
    int ans=0;
    for (int i = 0; i < slen; i++){
        while (j!=-1 && s[i]!=p[j+1]){
            j=next[j];
        }
        if(s[i]==p[j+1]) j++;
        if(j==plen-1){
            ans+=1;
            j=next[j];
        }
    }
    return ans;
}
int main(int argc, const char** argv) {
    // getNext("ab");
    int ans=KMP("abbababaab","ab");
    std::cout << "ans= "<<ans << std::endl;
    return 0;
}