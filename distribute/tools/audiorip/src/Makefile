#
# gcc Makefile for "audiorip"
#
# - Change the config part according to your system
# - This makefile creates directories: "obj", "bin"
#   Compilation result can be found there afterwards,
#   "make clean" deletes them
# 


### config start

#name of the executeable file
EXE = audiorip.exe

#compiler
CPP  = g++

OBJDIR = obj
OBJ = audiorip.o format.o
OBJ2 = $(OBJDIR)/audiorip.o $(OBJDIR)/format.o

BINDIR = bin
BIN = $(BINDIR)/$(EXE)

#library and include path (not needed on properly configured system)
#LIBS =  -L"c:/MinGW/lib"  
#INCS =  -I"c:/MinGW/include" 

#to create and delete directories
RM = rm -r -f
MKDIR = mkdir -v -p

#to remove debug info
STRIPBIN = strip $(BIN) -s

### config end






$(BIN): $(OBJ)
	$(MKDIR) $(BINDIR)
	$(CPP) $(OBJ2) -o $(BIN) $(INCS) $(LIBS) 
	$(STRIPBIN)

$(OBJ): $(OBJ:.o=.cpp)
	$(MKDIR) $(OBJDIR)
	$(CPP) -c $< -o $(OBJDIR)/$@



.PHONY : clean
clean :
	${RM} $(OBJDIR)
	${RM} $(BINDIR)

