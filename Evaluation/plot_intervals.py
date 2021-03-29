import numpy as np
import random
import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd
import time


def main():
    font = {'family' : 'normal',
        #'weight' : 'bold',
        'size'   : 22}

    plt.rc('font', **font)

    sns.set(font_scale=1.5)
    sns.set_style("ticks")
    plot_interval_mechanism_scores("plots/interval_scores.pdf")
    plot_interval_mechanism_scores_preserving("plots/interval_scores_preserving.pdf")



def plot_interval_mechanism_scores(output_path):
    intervals = [
                [(2, 2.5), (2.5, 5), (5, 7.5), (7.5, 9), (9, 10)],
                [(2, 2.5), (2.5, 5), (5, 7.5), (7.5, 9), (9, 10)],
                [(2, 3.6), (3.6, 5.2), (5.2, 6.8), (6.8, 8.4), (8.4, 10)],
                [(10, 15), (15, 25), (25, 35), (35, 45), (45, 50)]
                ]
    scores=     [
                [0, -1, -2, -3, -4],
                [-4, -3, -2, -1, 0],
                [-2, -1, 0, -1, -2],
                [-2, -1, 0, -1, -2]
                ]
    x_ticks =   [
                [2,4,6,8, 10],
                [2,4,6,8, 10],
                [2,4,6,8, 10],
                [10, 20, 30, 40, 50]
                ]
    titles =    ["Min", "Max", "Mean", "Sum"]
    results=    [2, 10, 6, 30]
    fig, ax = plt.subplots(nrows=1, ncols=4,constrained_layout=True)
    for i in range (0,4):
        for idx, value in enumerate(intervals[i]):
            ax[i].hlines(y=scores[i][idx], xmin=value[0], xmax=value[1], color="black")
        ax[i].set_yticks(scores[i])
        ax[i].set_xticks(x_ticks[i])
        #ax[i].tick_params(axis='x', labelrotation=90)
        ax[i].set_title(titles[i],fontsize=18)
        ax[i].axvline(results[i],color="b", ls='--',linewidth=4)
    fig.set_size_inches( 8, 2)
    plt.savefig(output_path,format="pdf")
    #plt.show()


def plot_interval_mechanism_scores_preserving(output_path):

    intervals = [(10, 15), (15, 25), (25, 30), (30, 35), (35, 45), (45, 50)]
                
    scores=[-2, -1, 0, -4, -8, -12]
    x_ticks =[10, 20, 30, 40, 50]
    titles =    ["Sum"]
    results=    [30]
    fig, ax = plt.subplots(nrows=1, ncols=1,constrained_layout=True)
    for idx, value in enumerate(intervals):
        ax.hlines(y=scores[idx], xmin=value[0], xmax=value[1], color="black")
    ax.set_yticks([0, - 4, - 8, -12])
    ax.set_xticks(x_ticks)
    #ax[i].tick_params(axis='x', labelrotation=90)
    ax.set_title(titles[0],fontsize=18)
    ax.axvline(results[0],color="b", ls='--', linewidth=4)
    fig.set_size_inches( 2 , 2)
    plt.savefig(output_path,format="pdf")
    #plt.show()



if __name__ == "__main__":
    #sys.argv = [sys.argv[0], 'ICC_alignment_approx_FINAL', 'alignment_ORIG_FINAL']
    main()#sys.argv)
