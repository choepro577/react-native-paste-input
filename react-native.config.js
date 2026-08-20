module.exports = {
    dependency: {
        platforms: {
            android: {
                libraryName: 'PasteTextInputSpecs',
                componentDescriptors: ['PasteTextInputComponentDescriptor'],
                cmakeListsPath: 'src/main/jni/CMakeLists.txt',
            },
        },
    },
};
