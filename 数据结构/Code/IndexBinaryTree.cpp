#include <iostream>
#include <vector>
using namespace std;
// 线索二叉树的实现
struct Node{
	int val;
	int LTag;
	int RTag;
	Node* lchild;
	Node* rchild;
	Node* prev;
	void show(){
		cout<<"val= "<<this->val<<"\tLTag= "<<this->LTag<<"\tRTag= "<<this->RTag;
	}
}*ThreadTree,ThreadNode,*pre; 
/*
	维基百科定义
	所有原本为空的右(孩子)指针改为指向该节点在中序序列中的后继，
	所有原本为空的左(孩子)指针改为指向该节点的中序序列的前驱
*/
void insert(Node* &root,int x){
	if(root==NULL){
		root=new Node;
		root->val=x;
		root->RTag=0;
		root->LTag=0;
		root->lchild=NULL;
		root->rchild=NULL;
		return;
	}
	if(x<root->val) insert(root->lchild,x);
	if(x>=root->val) insert(root->rchild,x);
}

void inThreadOrder(Node* root){
	if(root==NULL) return;
	inThreadOrder(root->lchild);
	cout<<root->val<<" ";
	if(root->lchild==NULL) {
		root->LTag=1;
		root->lchild=pre;
	}
	if(pre->rchild==NULL) {
		root->RTag=1;
		pre->rchild=root;
	}
	pre=root;
	inThreadOrder(root->rchild);
}
void inThreadTree(Node* &ptr,Node* root){
	ptr->LTag=0,ptr->RTag=1;
	// 右指针回指,构成循环链表
	ptr->rchild=ptr;
	if(root==NULL) ptr->lchild=ptr;
	else{
		// 首节点 左指针指向自己,右指针指向最后一个访问的节点
		ptr->lchild=root;
		pre=ptr;
		inThreadOrder(root);
		pre->rchild=ptr;// 右指针回指头部
		pre->RTag=1;// 最后一个一定是线索
		ptr->rchild=pre;
	}
}
void inOrder(Node* root){
	if(root==NULL) return;
	cout<<root->val<<" ";
	inOrder(root->lchild);
	inOrder(root->rchild);
}
int main(){
	int arr[9]={5,2,3,8,1,6,7,4,9};	
	Node* root=NULL;
	for (int i = 0; i < 9; i++)
		insert(root,arr[i]);
	Node* ptr=new Node;
	inThreadTree(ptr,root);
	return 0;
} 
