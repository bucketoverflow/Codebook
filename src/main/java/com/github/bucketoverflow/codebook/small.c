#include <stdio.h>

int main()
{
    char a[] = "Hello, World!";                                                 
    char b[20];                                                                 

    char *s = a, *d = b;                                                        
    while (*d++ = *s++)                                                         
        ;                                                                       
    return 0;                                                                   
}
