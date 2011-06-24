package sam_2

class Feature {

    String name
    String unit

    static hasMany = [featureGroups:FeatureGroup]

    static belongsTo = FeatureGroup

    static constraints = {
        name(unique:true, blank:false)
    }
}
