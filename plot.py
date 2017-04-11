import numpy as np
import matplotlib.pyplot as plot

def PlotFrequency(file):
    with open(file, 'r', encoding='utf8') as f:
        data = f.read()
        
    data = data.split('\n')

    x = [row.split(' ')[0] for row in data]
    y = [row.split(' ')[1] for row in data]
    #word = [row.split(' ')[2] for row in data]

    fig = plot.figure()
    
    ax1 = fig.add_subplot(211) #2 by 1 grid, slot 1
    ax1.set_title("Word Frequency vs Rank")    
    ax1.set_xlabel('Rank')
    ax1.set_ylabel('Frequency')
    ax1.plot(x, y, c = 'r', label = "Word Frequency")
    leg = ax1.legend()

    ax2 = fig.add_subplot(212) #2 by 1 grid, slot 2
    ax2.set_xlabel('Rank')
    ax2.set_ylabel('Frequency')
    ax2.set_yscale('log') #logarithmic scale
    ax2.set_xscale('log')
    ax2.plot(x, y, c = 'r', label = "Word Frequency")
    leg2 = ax2.legend()

    plot.show()


PlotFrequency("repository/WordFrequency.txt")
