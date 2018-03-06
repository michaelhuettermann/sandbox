
from setuptools import setup, find_packages

setup(
    name='python-package-example',
    version='0.4',
    packages=find_packages(exclude=['tests*']),
    license='MIT',
    description='a python package',
    long_description=open('README.txt').read(),
    install_requires=['numpy'],
    url='https://github.com/michaelhuettermann/sandbox',
    author='Michael Huettermann',
    author_email='michael@huettermann.net'
)
