# Bisaya++

Bisaya++ is a **strongly-typed, high-level interpreted Cebuano-based programming language** designed to teach programming fundamentals using native Cebuano syntax. It is ideal for educational use, especially for Cebuanos learning how to code for the first time.

## Table of Contents
- [Features](#features)
- [Language Overview](#language-overview)
- [Setup Instructions](#setup-instructions)
- [Usage Example](#usage-example)
- [Grammar](#grammar)
- [Contributing](#contributing)
- [License](#license)

---

## Features
- Cebuano-based keywords and syntax
- Strong typing with built-in types: `NUMERO`, `LETRA`, `TIPIK`, `TINUOD`
- Standard I/O: `IPAKITA` (print), `DAWAT` (input)
- Conditional structures: `KUNG`, `KUNG WALA`, `KUNG DILI`
- Loops via `ALANG SA`
- Expression parsing with logical, arithmetic, and relational operators
- Commenting with `--`

---

## Language Overview

### Sample Program
```bisaya
-- Sample program in Bisaya++
SUGOD  
  MUGNA NUMERO x, y, z=5  
  MUGNA LETRA a_1='n'  
  MUGNA TINUOD t="OO"  
  x=y=4 
  a_1='c' -- this is a comment 
  IPAKITA: x & t & z & $ & a_1 & [#] & "last" 
KATAPUSAN
```

### Output:
```
4OO5c#last
```

## Setup Instructions

### Requirements
- Java Development Kit (JDK 8 or higher)
- Git (optional)

### Clone the Repository
```bash
git clone https://github.com/your-username/bisaya-plus-plus.git
cd bisaya-plus-plus
```

### Compile the Interpreter
```bash
javac -d bin src/**/*.java
```

### Run the Interpreter
```bash
java -cp bin BisayaMain your_program.bpp
```

## Usage Example

Create a file named `example.bpp` with this content:
```bisaya
SUGOD
  MUGNA NUMERO a=10, b=20
  MUGNA TINUOD result
  result = (a < b UG b <> 0)
  IPAKITA: result
KATAPUSAN
```

Then run it:
```bash
java -cp bin BisayaMain example.bpp
```

Output:
```
OO
```

## Grammar

Refer to the Bisaya++ Language Grammar for the full specification of valid syntax.

Highlights:
- Programs begin with `SUGOD` and end with `KATAPUSAN`
- Variable declarations start with `MUGNA`
- Conditionals use `KUNG`, `KUNG WALA`, `KUNG DILI`
- Loops use `ALANG SA`
- Expressions support standard arithmetic and logical operators
- Comments start with `--`

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes (`git commit -m "Add feature"`)
4. Push to the branch (`git push origin feature/my-feature`)
5. Open a Pull Request

Please avoid using Python for interpreter logic, as per project guidelines.

## License

This project is licensed under the MIT License.
