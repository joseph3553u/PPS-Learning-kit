package com.example.model

data class CodeTemplate(
    val title: String,
    val description: String,
    val language: Language,
    val code: String,
    val sampleInput: String = ""
)

object TemplateLibrary {
    val templates = listOf(
        CodeTemplate(
            title = "Hello World (C)",
            description = "Basic entry point and printf in C",
            language = Language.C,
            code = """#include <stdio.h>

int main() {
    printf("Hello, C Programming World!\n");
    printf("Ready for compiling and debugging.\n");
    return 0;
}
""".trimIndent()
        ),
        CodeTemplate(
            title = "Interactive Input & Math (C)",
            description = "Using scanf to read numbers and calculate sum",
            language = Language.C,
            sampleInput = "15 25",
            code = """#include <stdio.h>

int main() {
    int a, b;
    printf("Enter two numbers: ");
    if (scanf("%d %d", &a, &b) == 2) {
        int sum = a + b;
        int diff = a - b;
        int prod = a * b;
        printf("\nResults:\n");
        printf("Sum: %d + %d = %d\n", a, b, sum);
        printf("Diff: %d - %d = %d\n", a, b, diff);
        printf("Product: %d * %d = %d\n", a, b, prod);
    } else {
        printf("Invalid input provided.\n");
    }
    return 0;
}
""".trimIndent()
        ),
        CodeTemplate(
            title = "Pointers & Arrays (C)",
            description = "Array traversal using pointer arithmetic",
            language = Language.C,
            code = """#include <stdio.h>

void print_array(int *arr, int size) {
    for (int i = 0; i < size; i++) {
        // Pointer arithmetic: *(arr + i) is identical to arr[i]
        printf("Element [%d] at %p = %d\n", i, (void*)(arr + i), *(arr + i));
    }
}

int main() {
    int numbers[] = {10, 20, 30, 40, 50};
    int size = sizeof(numbers) / sizeof(numbers[0]);
    printf("Traversing array of size %d via pointers:\n", size);
    print_array(numbers, size);
    return 0;
}
""".trimIndent()
        ),
        CodeTemplate(
            title = "Dynamic Memory & Structs (C)",
            description = "malloc, structs, and free in C",
            language = Language.C,
            code = """#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct {
    char name[32];
    int roll_no;
    float gpa;
} Student;

int main() {
    Student *s = (Student *)malloc(sizeof(Student));
    if (s == NULL) {
        printf("Memory allocation failed!\n");
        return 1;
    }

    strcpy(s->name, "Alex");
    s->roll_no = 101;
    s->gpa = 3.92f;

    printf("Student Record:\n");
    printf("Name: %s\n", s->name);
    printf("Roll Number: %d\n", s->roll_no);
    printf("GPA: %.2f\n", s->gpa);

    free(s);
    printf("Memory successfully freed.\n");
    return 0;
}
""".trimIndent()
        ),
        CodeTemplate(
            title = "Hello World & Streams (C++)",
            description = "Modern C++ std::cout, std::cin and streams",
            language = Language.CPP,
            code = """#include <iostream>
#include <string>

int main() {
    std::string greeting = "Hello, C++ World!";
    std::cout << greeting << std::endl;
    std::cout << "Compiled with C++17 support." << std::endl;
    return 0;
}
""".trimIndent()
        ),
        CodeTemplate(
            title = "STL Vector & Algorithm (C++)",
            description = "std::vector with std::sort and range-based for",
            language = Language.CPP,
            code = """#include <iostream>
#include <vector>
#include <algorithm>

int main() {
    std::vector<int> scores = {88, 42, 95, 71, 63, 100};
    std::cout << "Original scores: ";
    for (int s : scores) {
        std::cout << s << " ";
    }
    std::cout << std::endl;

    std::sort(scores.begin(), scores.end());

    std::cout << "Sorted scores:   ";
    for (int s : scores) {
        std::cout << s << " ";
    }
    std::cout << std::endl;
    return 0;
}
""".trimIndent()
        ),
        CodeTemplate(
            title = "Classes & OOP (C++)",
            description = "Encapsulation, constructor, and member methods",
            language = Language.CPP,
            code = """#include <iostream>
#include <string>

class BankAccount {
private:
    std::string owner;
    double balance;

public:
    BankAccount(std::string name, double initial)
        : owner(name), balance(initial) {}

    void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
            std::cout << "Deposited $" << amount << ". New balance: $" << balance << std::endl;
        }
    }

    void display() const {
        std::cout << "Account [" << owner << "] Balance: $" << balance << std::endl;
    }
};

int main() {
    BankAccount acct("Jordan", 500.0);
    acct.display();
    acct.deposit(250.0);
    return 0;
}
""".trimIndent()
        )
    )

    val cheatSheet = listOf(
        "printf(\"%d\", val)" to "Decimal integer",
        "printf(\"%f\", val)" to "Floating point number (use %.2f for 2 decimals)",
        "printf(\"%s\", str)" to "Null-terminated string",
        "printf(\"%c\", ch)" to "Single character",
        "scanf(\"%d\", &x)" to "Read int (remember the '&' address operator!)",
        "std::cin >> x" to "C++ input stream (no & required)",
        "std::cout << x" to "C++ output stream",
        "malloc(size)" to "Allocate heap memory (returns void*)",
        "free(ptr)" to "Deallocate heap memory to prevent memory leaks",
        "ptr->field" to "Access struct/class member through pointer"
    )
}
