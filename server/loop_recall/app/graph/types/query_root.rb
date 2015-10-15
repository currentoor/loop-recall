QueryRoot = GraphQL::ObjectType.define do
  name "Query"
  description "The query root for this schema"

  field :card do
    type CardType
    description "Find a User by id"
    argument :id, !types.ID
    resolve -> (object, arguments, context) {
      Card.find(arguments["id"])
    }
  end
end
