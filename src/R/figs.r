library(plotly)
library(deSolve)

axx = list(nticks=5, range=c(-2, 2))
axy = list(nticks=5, range=c(-2, 2))
axz = list(nticks=5, range=c(-4, 4))
axx.tight = list(nticks=5, range=c(0,1))
axy.tight = list(nticks=5, range=c(0,1))
axz.tight = list(nticks=5, range=c(0, 1))

data.full = data.frame(x=as.numeric(gl(400, 101))/100-2, y=seq(-2, 2, length.out=101))
data.full$z = with(data.full, x*y)

data.tight = data.frame(x=as.numeric(gl(100, 101))/100, y=seq(0, 1, length.out=101))
data.tight$z = with(data.tight, x*y)

fig1 = plot_ly(x = ~x, y = ~y, z = ~z, data.full, type = 'mesh3d') 
fig1 = fig %>% layout(scene = list(xaxis=axx, yaxis=axy, zaxis=axz,
                                  aspectmode='manual',
                                  aspectratio = list(x=1, y=1, z=1)))
fig1

fig2 = plot_ly(x = ~x, y = ~y, z = ~z, data.tight, type = 'mesh3d') 
fig2 = fig2 %>% layout(scene = list(xaxis=axx, yaxis=axy, zaxis=axz,
                                  aspectmode='manual',
                                  aspectratio = list(x=1, y=1, z=1)))
fig2

fig3 = plot_ly(x = ~x, y = ~y, z = ~z, data.tight, type = 'mesh3d') 
trace = data.frame(x=seq(1,0,length.out=101), y=seq(0,1,length.out=101))
trace$z = with(trace, x*y)
fig3 = fig3 %>% add_trace(type="scatter3d", mode='lines', x=trace$x, y=trace$y, z=trace$z,
                              line = list(width = 6, color = 'red', reverscale = FALSE))
for (x in seq(0,1,by=0.2)) {
    trace = data.frame(x=x, y=seq(0,1,length.out=101))
    trace$z = with(trace, x*y)
    fig3 = fig3 %>% add_trace(type="scatter3d", mode='lines', x=trace$x, y=trace$y, z=trace$z,
                              line = list(width = 6, color = 'green', reverscale = FALSE))
}
grad = data.frame(x=rep(seq(0,1,by=0.2), each=6), y=seq(0,1,by=0.2))
fig3 = fig3 %>% add_trace(type="cone", x=grad$x, y=grad$y, z=0, u=-grad$y/10, v=-grad$x/10, w=0)

for (x0 in seq(0,1,by=0.1)) {
    trace = ode(c(x=x0, y=0), seq(0,20e-1,by=5e-2), function(t, y, ...){list(c(y[2], y[1]), c())})
    fig3 = fig3 %>% add_trace(type="scatter3d", mode='lines', x=trace[,2], y=trace[,3], z=0)
}

fig3 = fig3 %>% layout(scene = list(xaxis=axx.tight, yaxis=axy.tight, zaxis=axz.tight,
                                  aspectmode='manual',
                                  aspectratio = list(x=1, y=1, z=1)))

fig3


dome = data.tight
dome$z = with(dome, x*(1-x) + y*(1-y))
fig4 = plot_ly(x = ~x, y = ~y, z = ~z, dome, type = 'mesh3d')
x = seq(0,1,by=0.05)
fig4 = fig4 %>% add_trace(type="scatter3d", mode="lines", x=x, y=0, z=x*(1-x), color='red')
u = as.data.frame(outer(c(-1, 0, 2), c(x=0.02, y=0.15) ))
v = as.data.frame(outer(c(-1, 0, 1), c(x=-0.05, y=0.1) ))
fig4 = fig4 %>% add_trace(type="scatter3d", mode="points", x=0.4 + u$x, y=0.6 + u$y, z=0, color='black')
fig4 = fig4 %>% add_trace(type="scatter3d", mode="points", x=0.4 + v$x, y=0.6 + v$y, z=0, color='blue')

x = 0.4 + u$x
y = 0.6 + u$y
fig4 = fig4 %>% add_trace(type="scatter3d", mode="points", x=x, y=y, z=0, color='black')
fig4 = fig4 %>% add_trace(type="scatter3d", mode="points", x=x, y=y, z=x*(1-x)+y*(1-y), color='black')
x = 0.4 + v$x
y = 0.6 + v$y
fig4 = fig4 %>% add_trace(type="scatter3d", mode="points", x=0.4 + v$x, y=0.6 + v$y, z=0, color='blue')
fig4 = fig4 %>% add_trace(type="scatter3d", mode="points", x=0.4 + v$x, y=0.6 + v$y, z=x*(1-x)+y*(1-y), color='blue')

fig4 = fig4 %>% layout(scene = list(xaxis=axx.tight, yaxis=axy.tight, zaxis=list(nticks=5, range=c(0,0.5)) ,
                                  aspectmode='manual',
                                  aspectratio = list(x=1, y=1, z=1)))

fig4
