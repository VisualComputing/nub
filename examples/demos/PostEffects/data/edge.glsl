varying vec4 vertTexCoord;

uniform sampler2D tex;
uniform vec2 aspect;
                                        
vec2 texel = vec2(aspect.x, aspect.y);

mat3 G[9];

mat3 G0 = mat3( 0.5/sqrt(2.0), 0, -0.5/sqrt(2.0), 0.5, 0, -0.5, 0.5/sqrt(2.0), 0, -0.5/sqrt(2.0) );
mat3 G1 = mat3( 0.5/sqrt(2.0), 0.5, 0.5/sqrt(2.0), 0, 0, 0, -0.5/sqrt(2.0), -0.5, -0.5/sqrt(2.0) );
mat3 G2 = mat3( 0, -0.5/sqrt(2.0), 0.5, 0.5/sqrt(2.0), 0, -0.5/sqrt(2.0), -0.5, 0.5/sqrt(2.0), 0 );
mat3 G3 = mat3( 0.5, -0.5/sqrt(2.0), 0, -0.5/sqrt(2.0), 0, 0.5/sqrt(2.0), 0, 0.5/sqrt(2.0), -0.5 );
mat3 G4 = mat3( 0, 0.5, 0, -0.5, 0, -0.5, 0, 0.5, 0);
mat3 G5 = mat3( -0.5, 0, 0.5, 0, 0, 0, 0.5, 0, -0.5 );
mat3 G6 = mat3( 1.0/6.0, -1.0/3.0, 1.0/6.0, -1.0/3.0, 2.0/3.0, -1.0/3.0, 1.0/6.0, -1.0/3.0, 1.0/6.0 );
mat3 G7 = mat3( -1.0/3.0, 1.0/6.0, -1.0/3.0, 1.0/6.0, 2.0/3.0, 1.0/6.0, -1.0/3.0, 1.0/6.0, -1.0/3.0);
mat3 G8 = mat3( 1.0/3.0, 1.0/3.0, 1.0/3.0, 1.0/3.0, 1.0/3.0, 1.0/3.0, 1.0/3.0, 1.0/3.0, 1.0/3.0 );
                                                
void main(void)
{
        G[0] = G0;
        G[1] = G1;
        G[2] = G2;
        G[3] = G3;
        G[4] = G4;
        G[5] = G5;
        G[6] = G6;
        G[7] = G7;
        G[8] = G8;

        mat3 I;
        float cnv[9];
        vec3 s;
            
        for (float i=0.0; i<3.0; i++)
        {
                for (float j=0.0; j<3.0; j++)
                {
                        s = texture2D(tex, vertTexCoord.st + texel * vec2(i-1.0,j-1.0)).rgb;
                        I[int(i)][int(j)] = length(s); 
                }
        }

        for (int i=0; i<9; i++)
        {
                float dp3 = dot(G[i][0], I[0]) + dot(G[i][1], I[1]) + dot(G[i][2], I[2]);
                cnv[i] = dp3 * dp3; 
        }

        float M = (cnv[0] + cnv[1]) + (cnv[2] + cnv[3]);
        float S = (cnv[4] + cnv[5]) + (cnv[6] + cnv[7]) + (cnv[8] + M); 

        gl_FragColor = vec4(vec3(sqrt(M/S)), 1.0);
}
