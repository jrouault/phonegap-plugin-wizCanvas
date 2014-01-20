#import "EJBindingBase.h"
#import "EJGLProgram2D.h"

typedef enum {
	kEJGLUniform1f,
	kEJGLUniform2f,
	kEJGLUniform3f,
    kEJGLUniform4f,
	kEJGLUniform1i,
	kEJGLUniform2i,
	kEJGLUniform3i,
    kEJGLUniform4i,
} EJGLUniform;

typedef struct {
    int count;
    // TODO: Keep type? No longer useful with function pointers
    EJGLUniform type;
	void *values;
    void (*glUniformFunction)(GLint, GLsizei, const void *);
} EJUniform;

@interface EJBindingMaterial : EJBindingBase {
    // TODO: Should it wrap a Material object?
	EJGLProgram2D *program;
    NSString *shaderName;
    BOOL hasChanged;
    NSMutableDictionary *uniforms;
}

- (void)assignProgramWithName:(NSString *)name;

@property (readonly, nonatomic) EJGLProgram2D *program;
@property (nonatomic) BOOL hasChanged;
@property (readonly, nonatomic) NSMutableDictionary *uniforms;

@end
