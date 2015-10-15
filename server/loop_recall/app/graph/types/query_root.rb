QueryRoot = GraphQL::ObjectType.define do
  name "Query"
  description "The query root for this schema"

  field :card do
    argument :id, !types.ID
    resolve -> (object, arguments, context) {
      Card.find(arguments["id"])
    }
  end

  field :cards do
    type [CardType]
    resolve -> (object, arguments, context) {
      Card.all
    }
  end

  field :user do
    type UserType
    description "Find a User by id"
    argument :id, !types.ID
    resolve -> (object, arguments, context) {
      User.find(arguments["id"])
    }
  end
end
