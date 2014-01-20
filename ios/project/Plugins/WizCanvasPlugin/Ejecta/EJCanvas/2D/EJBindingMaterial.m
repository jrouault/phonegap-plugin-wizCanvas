#import "EJBindingMaterial.h"

#import "EJConvertWebGL.h"

@implementation EJBindingMaterial
@synthesize program;
@synthesize hasChanged;
@synthesize uniforms;

- (void)assignProgramWithName:(NSString *)name {
    // Check first if a shader program with the specified name exists
    EJSharedOpenGLContext *sharedGLContext = scriptView.openGLContext;
    NSString *propertySuffix = @"glProgram2D";
    NSString *selectorName = [propertySuffix stringByAppendingString:name];
    SEL programSelector = NSSelectorFromString(selectorName);
    
    if ([sharedGLContext respondsToSelector:programSelector]) {
        program = [sharedGLContext performSelector:programSelector];
        if (program) {
            shaderName = [name retain];
        }
    } else {
        return;
    }
}

- (void)dealloc {
    [shaderName release];
    // Remove the uniforms allocated manually
    for (id key in uniforms) {
        NSValue *value = [uniforms objectForKey:key];
        if (value) {
            EJUniform *oldUniform;
            [value getValue:&oldUniform];
            free(oldUniform->values);
            free(oldUniform);
        }
    }
    [uniforms release];
    uniforms = nil;
    
	[super dealloc];
}

EJ_BIND_GET(shader, ctx) {
	JSStringRef shader = JSStringCreateWithUTF8CString([shaderName UTF8String]);
	JSValueRef ret = JSValueMakeString(ctx, shader);
	JSStringRelease(shader);
	return ret;
}

EJ_BIND_SET(shader, ctx, value) {
	NSString *newShaderName = JSValueToNSString(ctx, value);

	// Same as the old shader name? Nothing to do here
	if ([shaderName isEqualToString:newShaderName]) {
        return;
    }

    hasChanged = true;
    
	// Release the old shader name and the program?
	if (shaderName) {
		[shaderName release];
		shaderName = nil;
        
        program = nil;
	}
	
	if (!JSValueIsNull(ctx, value) && [newShaderName length]) {
		[self assignProgramWithName:newShaderName];
	}
}

// TODO: Support Matrix uniforms too
EJ_BIND_FUNCTION(setUniform, ctx, argc, argv) {
	if (argc < 2) {
        return NULL;
    }
    
    if (JSValueIsNull(ctx, argv[0]) || JSValueIsNull(ctx, argv[1])) {
        return NULL;
    }
    
    NSString *uniform = JSValueToNSString(ctx, argv[0]);
    NSString *uniformType = JSValueToNSString(ctx, argv[1]);

    if ([uniform length] && [uniformType length])  {
        GLsizei count = 0;
        void *values;
        size_t uniformArraySize;

        // TODO: Capture error?
        // Check if the uniform type is recognized
        NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:@"gluniform([1234][f|i])v?"
                                                                               options:NSRegularExpressionCaseInsensitive
                                                                                 error:nil];
        NSUInteger numberMatches = [regex numberOfMatchesInString:uniformType
                                                          options:0
                                                            range:NSMakeRange(0, [uniformType length])];
        if (numberMatches == 1) {
            // Extract the uniform type
            NSString *componentType = [regex stringByReplacingMatchesInString:uniformType
                                                                      options:0
                                                                        range:NSMakeRange(0, [uniformType length])
                                                                 withTemplate:@"$1"];

            // TODO: Possible leaks with JSValueToGLfloatArray()
            if ([componentType hasSuffix:@"f"]) {
                values = JSValueToGLfloatArray(ctx, argv[2], 1, &count);
                uniformArraySize = count * sizeof(GLfloat);
            } else if ([uniformType hasSuffix:@"i"]) {
                values = JSValueToGLintArray(ctx, argv[2], 1, &count);
                uniformArraySize = count * sizeof(GLint);
            }
            
            if (count > 0) {
                if (!uniforms) {
                    uniforms = [[NSMutableDictionary alloc] init];
                }

                EJUniform *newUniform = malloc(sizeof(EJUniform));

                void *uniformValues = malloc(uniformArraySize);;
                memcpy(uniformValues, values, uniformArraySize);
                newUniform->values = uniformValues;
                
                if([componentType isEqualToString:@"1f"]) {
                    newUniform->count = count;
                    newUniform->type = kEJGLUniform1f;
                    newUniform->glUniformFunction = glUniform1fv;
                } else if ([componentType isEqualToString:@"2f"]) {
                    newUniform->count = floor((float)count/2);
                    newUniform->type = kEJGLUniform2f;
                    newUniform->glUniformFunction = glUniform2fv;
                } else if ([componentType isEqualToString:@"3f"]) {
                    newUniform->count = floor((float)count/3);
                    newUniform->type = kEJGLUniform3f;
                    newUniform->glUniformFunction = glUniform3fv;
                } else if ([componentType isEqualToString:@"4f"]) {
                    newUniform->count = floor((float)count/4);
                    newUniform->type = kEJGLUniform4f;
                    newUniform->glUniformFunction = glUniform4fv;
                } else if ([componentType isEqualToString:@"1i"]) {
                    newUniform->count = count;
                    newUniform->type = kEJGLUniform1i;
                    newUniform->glUniformFunction = glUniform1iv;
                } else if ([componentType isEqualToString:@"2i"]) {
                    newUniform->count = floor((float)count/2);
                    newUniform->type = kEJGLUniform2i;
                    newUniform->glUniformFunction = glUniform2iv;
                } else if ([componentType isEqualToString:@"3i"]) {
                    newUniform->count = floor((float)count/3);
                    newUniform->type = kEJGLUniform3i;
                    newUniform->glUniformFunction = glUniform3iv;
                } else if ([componentType isEqualToString:@"4i"]) {
                    newUniform->count = floor((float)count/4);
                    newUniform->type = kEJGLUniform4i;
                    newUniform->glUniformFunction = glUniform4iv;
                }
                
                // Check if the uniform was already set and free it correctly if it was
                NSValue *value = [uniforms objectForKey:uniform];
                if (value) {
                    EJUniform *oldUniform;
                    [value getValue:&oldUniform];
                    free(oldUniform->values);
                    free(oldUniform);
                }

                [uniforms setObject:[NSValue valueWithPointer:newUniform] forKey:uniform];

                hasChanged = true;
            }
        } else {
            NSLog(@"Warning: Uniform type not recognized. Matrix uniforms are not currently supported.");
        }
    }
    
    return NULL;
}

@end
