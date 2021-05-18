#include <stdio.h>
int f()
{
  if (x) {
   return 2;
  } else if (y) {
   if (a) {
    if (b) {
      if (c) {
        return 1;
      }
    }
   }
  }

 while (x < 2) {
   if (r) {
    if (z) {
     continue;
    }
   }
 }

  return 0;
} 

int main(int argc, char *argv[])
{
  return f();
}
