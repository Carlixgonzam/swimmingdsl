module Runner

import WebAPI;
import IO;
import List;

// This runner executes WebAPI commands
// Usage: pass command and arguments as program arguments
void main(list[str] args) {
  if (size(args) == 0) {
    println("{\"success\":false,\"error\":\"No arguments provided\"}");
    return;
  }
  
  // Call WebAPI main with the arguments
  WebAPI::main(args);
}
