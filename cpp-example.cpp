std::string one_more(const char* a) {
  return std::string(a);
}

std::string deeper(char* a, std::string type) {
  return one_more(a);
}

void parse(char *a, std::string type) {
  return deeper(a, type);
}

void without_strings(int a, int b) {
  return;
}

int another(int a, double c) {
  return a;
}

void lots_of_args(int a, int b, int c, double d, float e, const char* f) {
  return a + b;
}

void finally(std::string a) {
  print(a);
}

int main() { }

inf f(){return 0;}

