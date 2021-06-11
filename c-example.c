#include <stdio.h>
int f(){return 0;} 
int main(int argc, char *argv[])
{
  /* I'm adding a comment, and still failing the goals */
  if (introduce_a_problem) {
    if (a) {
      if (b) {
        if (c) {
          if (d) {
            return 1;
          }
        }
      }
    }
  }

  return 0;
}
