# -*- coding: utf-8 -*-
"""style_classifier_multi_label_classification.ipynb

Automatically generated by Colaboratory.

Original file is located at
    https://colab.research.google.com/drive/1KrcoLlkWAlxwnfsAoRt2yxXGgd23U2ze

# load library
"""
#torch

from torch.nn import Parameter
import torch
import torch.optim
import torch.nn as nn
import torch.nn.parallel
import torch.nn.functional as F

import torchvision.models as models
import torchvision.transforms as transforms

#import torch.utils.data as Data
#import torch.backends.cudnn as cudnn

#numpy
import numpy as np

#pandas
import pandas as pd

#utility
import pickle

import math

from PIL import Image

from util import *

"""# define GCNResNet"""

class GraphConvolution(nn.Module):

    def __init__(self, in_features, out_features, bias=False):
        super(GraphConvolution, self).__init__()
        self.in_features = in_features
        self.out_features = out_features
        self.weight = Parameter(torch.Tensor(in_features, out_features))
        if bias:
            self.bias = Parameter(torch.Tensor(1, 1, out_features))
        else:
            self.register_parameter('bias', None)
        self.reset_parameters()

    def reset_parameters(self):
        stdv = 1. / math.sqrt(self.weight.size(1))
        self.weight.data.uniform_(-stdv, stdv)
        if self.bias is not None:
            self.bias.data.uniform_(-stdv, stdv)

    def forward(self, input, adj):
        support = torch.matmul(input, self.weight)
        output = torch.matmul(adj, support)
        if self.bias is not None:
            return output + self.bias
        else:
            return output

    def __repr__(self):
        return self.__class__.__name__ + ' (' \
               + str(self.in_features) + ' -> ' \
               + str(self.out_features) + ')'


class GCNResnet(nn.Module):
    def __init__(self, model, num_classes, in_channel=300, t=0, adj_file=None):
        super(GCNResnet, self).__init__()
        self.features = nn.Sequential(
            model.conv1,
            model.bn1,
            model.relu,
            model.maxpool,
            model.layer1,
            model.layer2,
            model.layer3,
            model.layer4,
        )
        self.num_classes = num_classes
        # self.pooling = nn.MaxPool2d(14, 14)
        self.pooling = nn.MaxPool2d(7, 7)

        self.gc1 = GraphConvolution(in_channel, 1024)
        self.gc2 = GraphConvolution(1024, 2048)
        self.relu = nn.LeakyReLU(0.2)

        _adj = gen_A(num_classes, t, adj_file)
        self.A = Parameter(torch.from_numpy(_adj).float())
        #         # image normalization
        self.image_normalization_mean = [0.485, 0.456, 0.406]
        self.image_normalization_std = [0.229, 0.224, 0.225]

    def forward(self, feature, inp):
        feature = self.features(feature)
        feature = self.pooling(feature)
        feature = feature.view(feature.size(0), -1)
        inp = inp[0]
        adj = gen_adj(self.A).detach()
        x = self.gc1(inp, adj)
        x = self.relu(x)
        x = self.gc2(x, adj)
        x = x.transpose(0, 1)
        x = torch.matmul(feature, x)
        return x

    def get_config_optim(self, lr, lrp):
        return [
                {'params': self.features.parameters(), 'lr': lr * lrp},
                {'params': self.gc1.parameters(), 'lr': lr},
                {'params': self.gc2.parameters(), 'lr': lr},
                ]


def gcn_resnet101(num_classes, t, pretrained=True, adj_file=None, in_channel=300):
    model = models.resnet101(pretrained=pretrained)
    return GCNResnet(model, num_classes, t=t, adj_file=adj_file, in_channel=in_channel)

"""# initialize hyperparameter"""

image_size = 224

change_class_style = {0: "classic",
         1: "preppy", 
         2: "manish",
         3: "tomboy", 
         4: "feminine",
         5: "romantic",
         6: "sexy",
         7: "hippie",
         8: "western",
         9: "oriental",
         10: "modern",
         11: "sophisticated",
         12: "avantgarde",
         13: "country",
         14: "resort",
         15: "genderless",
         16: "sporty",
         17: "retro",
         18: "hiphop",
         19: "kitsch",
         20: "punk",
         21: "street",
         22: "military"
         }

"""# load model"""

num_classes_style = 23

adj = "./style/single_adj_file.pkl"

model_single = gcn_resnet101(num_classes=num_classes_style, t=0.03, adj_file=adj)

"""# data augmentation"""

normalize = transforms.Normalize(mean = model_single.image_normalization_mean, std = model_single.image_normalization_std)

val_transform = transforms.Compose([
    Warp(image_size),
    transforms.ToTensor(),
    normalize,
])



"""# best model"""

resume = "./style/single_best_model_v2.pth"

checkpoint = torch.load(resume, map_location=torch.device('cpu'))

model_single.load_state_dict(checkpoint)

model_single.eval()

"""word2vec"""

wordvec = "./style/custom_word2vec.pkl"

with open(wordvec, 'rb') as f:
    inp = pickle.load(f)

inp = torch.tensor(inp).unsqueeze(0)
inp_var = torch.autograd.Variable(inp).float().detach()  # one hot