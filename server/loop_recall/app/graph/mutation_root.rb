MutationRoot = GraphQL::ObjectType.define do
  name "Mutation"
  description "The mutation root for this schema"

  # field :createUser, UserType do
  #   argument :email, !types.String
  #   argument :password, !types.String
  #   resolve -> (object, args, context) {
  #     User.create(
  #       email: args["email"],
  #       password: args["password"],
  #     )
  #   }
  # end

  field :createCard, CardType do
    # Ex: "mutation bar { createCard(question: \"who?\", answer: \"me!\", user_id: \"1\") {question, answer} }"
    argument :user_id, !types.ID
    argument :question, !types.String
    argument :answer, !types.String
    resolve -> (object, args, context) {
      Card.transaction do
        user = User.find(args['user_id'])

        Card.create!(
          question: args["question"],
          answer: args["answer"]
        ).tap do |card|
          user.cards << card
        end
      end
    }
  end

  field :updateCard, CardType do
    # Ex: "mutation bar { updateCard(question: \"?????\", answer: \"!!!!\", id: \"1\") {id} }"
    argument :id, !types.ID
    argument :question, types.String
    argument :answer, types.String
    resolve -> (object, args, context) {
      id, question, answer = [
        args['id'],
        args['question'],
        args['answer']
      ]

      Card.find(id).tap do |card|
        card.question = question if question
        card.answer = answer if answer
        card.save!
      end
    }
  end

  field :deleteCard, CardType do
    # Ex: "mutation bar { deleteCard(id: \"1\") {id} }"
    argument :id, !types.ID
    resolve -> (object, args, context) {
      id = args['id']
      Card.find(id).delete
    }
  end
end
