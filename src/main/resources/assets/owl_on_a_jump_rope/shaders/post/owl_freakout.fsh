#version 330

// The "everything goes wrong" post-process that follows the nuke flash: the world is crushed to
// harsh black-and-white, brutally over-contrasted, savagely over-sharpened (edges scream and halo)
// and posterized so tonal detail falls apart - the picture visibly freaks out and degrades.

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

const vec3 LUMA = vec3(0.299, 0.587, 0.114);
const float CONTRAST = 2.6;   // slams tones toward pure black / pure white around mid-grey
const float SHARPEN  = 2.4;   // unsharp-mask strength - high enough to ring and glitch on edges
const float LEVELS   = 5.0;   // posterization steps - crushes the gradient, "worsens" the image

void main() {
    vec2 oneTexel = 1.0 / InSize;

    vec3 c  = texture(InSampler, texCoord).rgb;
    vec3 up = texture(InSampler, texCoord + vec2(0.0, -oneTexel.y)).rgb;
    vec3 dn = texture(InSampler, texCoord + vec2(0.0,  oneTexel.y)).rgb;
    vec3 lf = texture(InSampler, texCoord + vec2(-oneTexel.x, 0.0)).rgb;
    vec3 rt = texture(InSampler, texCoord + vec2( oneTexel.x, 0.0)).rgb;

    // Unsharp mask: push the centre away from its neighbourhood average, hard.
    vec3 sharp = c * (1.0 + 4.0 * SHARPEN) - (up + dn + lf + rt) * SHARPEN;

    // Collapse to a single grey value.
    float g = dot(clamp(sharp, 0.0, 1.0), LUMA);

    // Merciless contrast about mid-grey.
    g = clamp((g - 0.5) * CONTRAST + 0.5, 0.0, 1.0);

    // Posterize into a few flat bands so smooth shading breaks up.
    g = floor(g * LEVELS + 0.5) / LEVELS;

    fragColor = vec4(vec3(g), 1.0);
}
